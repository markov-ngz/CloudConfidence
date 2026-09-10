package ngz.cloudconfidence.convertgrib2.parquetconverter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import ngz.cloudconfidence.convertgrib2.ProcessResult;
import ngz.cloudconfidence.convertgrib2.grib2.GridDatasetProcessor;
import ngz.cloudconfidence.convertgrib2.grib2.exception.DatasetProcessingException;
import ngz.extraction.core.filename.FileNameGenerator;
import ngz.extraction.core.sink.SunkFile;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts GRIB files to Parquet format using Avro serialization. Supports both single-file and
 * parallel multi-file conversion. Each file conversion manages its own Parquet writer lifecycle.
 */
public class GribParquetConverter implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(GribParquetConverter.class);
    private static final int DEFAULT_THREAD_POOL_SIZE =
            Math.max(2, Runtime.getRuntime().availableProcessors() * 4);

    private final GridDatasetProcessor processor;
    private final FileNameGenerator fileNameGenerator;
    private final Configuration hadoopConf;
    private final String outputDir;
    private final ExecutorService executorService;

    /**
     * Constructs a {@code GribParquetConverter} with a default thread pool sized to the number of
     * available processors.
     *
     * @param processor the dataset processor used to read GRIB grid data
     * @param hadoopConf the Hadoop configuration for filesystem access
     * @param outputDir the output directory where Parquet files will be written
     * @param fileNameGenerator the strategy used to generate output file names
     */
    public GribParquetConverter(
            GridDatasetProcessor processor,
            Configuration hadoopConf,
            String outputDir,
            FileNameGenerator fileNameGenerator) {
        this.processor = processor;
        this.fileNameGenerator = fileNameGenerator;
        this.hadoopConf = hadoopConf;
        this.outputDir = outputDir;
        this.executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * Constructs a {@code GribParquetConverter} with an explicit thread pool size. Prefer this
     * constructor when running in a constrained environment or when I/O throughput needs to be
     * capped (e.g. on HDFS with limited bandwidth).
     *
     * @param processor the dataset processor used to read GRIB grid data
     * @param hadoopConf the Hadoop configuration for filesystem access
     * @param outputDir the output directory where Parquet files will be written
     * @param fileNameGenerator the strategy used to generate output file names
     * @param threadPoolSize the number of threads to use for parallel file processing
     */
    public GribParquetConverter(
            GridDatasetProcessor processor,
            Configuration hadoopConf,
            String outputDir,
            FileNameGenerator fileNameGenerator,
            int threadPoolSize) {
        this.processor = processor;
        this.fileNameGenerator = fileNameGenerator;
        this.hadoopConf = hadoopConf;
        this.outputDir = outputDir;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * Converts multiple GRIB files to Parquet in parallel, collecting all results. Files are
     * processed concurrently using the internal thread pool. Failures on individual files are
     * captured as {@link ProcessResult#failure} entries and do not abort the remaining conversions.
     *
     * @param files the array of {@link SunkFile} descriptors to convert
     * @return a list of {@link ProcessResult}, one per input file, preserving submission order
     */
    public List<ProcessResult> convertMultiple(SunkFile[] files) {
        List<CompletableFuture<ProcessResult>> futures =
                Arrays.stream(files)
                        .map(
                                file ->
                                        CompletableFuture.supplyAsync(
                                                () -> {
                                                    try {
                                                        return convert(file);
                                                    } catch (IOException e) {
                                                        LOG.error(
                                                                "Failed to process URI {}",
                                                                file.getUri(),
                                                                e);
                                                        return ProcessResult.failure(
                                                                file.getUri(), e.getMessage());
                                                    }
                                                },
                                                executorService))
                        .toList();

        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    /**
     * Converts a single GRIB file to Parquet format. A dedicated {@link ParquetWriter} is created
     * and closed within this call, making it safe to invoke concurrently from multiple threads
     * without any shared mutable state.
     *
     * @param file the GRIB source file descriptor, including URI and metadata
     * @return a {@link ProcessResult} describing the outcome
     * @throws IOException if an I/O or processing error occurs
     */
    public ProcessResult convert(SunkFile file) throws IOException {
        Map<String, String> fileMetadata = file.getMetadata();
        GridRecordFactory recordFactory =
                new GridRecordFactory.Builder()
                        .previnum(fileMetadata.get("previnum"))
                        .model(fileMetadata.get("model"))
                        .grid(fileMetadata.get("grid"))
                        .packageName(fileMetadata.get("package"))
                        .referenceTime(fileMetadata.get("referenceTime"))
                        .time(fileMetadata.get("time"))
                        .build();

        Path outputPath = createOutputPath(this.outputDir, file.getUri());
        LOG.info("Begin processing URI : {}", file.getUri());

        try (ParquetWriter<GenericRecord> writer = createParquetWriter(outputPath)) {
            processor.process(
                    file.getUri(),
                    gridItem -> {
                        GenericRecord record = recordFactory.fromGridItem(gridItem);
                        writer.write(record);
                    });

            LOG.info(
                    "Parquet file written successfully for URI: {}, at OutputPath: {}",
                    file.getUri(),
                    outputPath.getName());
            return ProcessResult.success(file.getUri(), outputPath.getName());

        } catch (DatasetProcessingException | IOException e) {
            LOG.error("Pipeline failed for URI: {}", file.getUri(), e);
            throw new IOException("Failed to process " + file.getUri(), e);
        }
    }

    /**
     * Derives the output {@link Path} for a given source URI by stripping the file extension and
     * delegating name generation to the {@link FileNameGenerator}.
     *
     * @param outputDir the base output directory
     * @param uri the source file URI used to extract the base file name
     * @return the full output {@link Path} for the resulting Parquet file
     */
    private Path createOutputPath(String outputDir, String uri) {
        String fileName = Paths.get(uri).getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        String fileNameWithoutExtension =
                (lastDotIndex != -1) ? fileName.substring(0, lastDotIndex) : fileName;

        String outputFileName =
                fileNameGenerator.generateFileName(
                        fileNameWithoutExtension.replaceAll("[.,:]", ""), "parquet");

        return new Path(outputDir, outputFileName);
    }

    /**
     * Creates a new {@link ParquetWriter} targeting the specified output path, configured with the
     * Avro schema from {@link GridRecordFactory#SCHEMA}. Each call returns an independent writer
     * instance; callers are responsible for closing it (preferably via try-with-resources).
     *
     * @param outputPath the Hadoop {@link Path} where the Parquet file will be written
     * @return a configured and open {@link ParquetWriter}
     * @throws IOException if the writer cannot be initialized
     */
    private ParquetWriter<GenericRecord> createParquetWriter(Path outputPath) throws IOException {
        return AvroParquetWriter.<GenericRecord>builder(
                        HadoopOutputFile.fromPath(outputPath, this.hadoopConf))
                .withSchema(GridRecordFactory.SCHEMA)
                .withConf(this.hadoopConf)
                .build();
    }

    /**
     * Shuts down the internal thread pool, waiting up to 60 seconds for in-flight conversions to
     * complete before forcing a shutdown. Should be called when the converter is no longer needed,
     * preferably via a try-with-resources block.
     *
     * @throws IOException not thrown directly, declared for {@link Closeable} compliance
     */
    @Override
    public void close() throws IOException {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                LOG.warn("Thread pool did not terminate cleanly, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
