package ngz.cloudconfidence.convertgrib2.parquetconverter;

import ngz.cloudconfidence.convertgrib2.grib2.GridDatasetProcessor;
import ngz.cloudconfidence.convertgrib2.grib2.NetCdfGridDatasetProcessor;
import ngz.cloudconfidence.convertgrib2.grib2.TemporaryFileDownloader;
import ngz.extraction.core.filename.FileNameGenerator;
import ngz.extraction.core.filename.SimpleFileNameGenerator;
import org.apache.hadoop.conf.Configuration;

public class GribParquetConverterFactory {

    public static GribParquetConverter create(
            Configuration hadoopConfig, String jobId, String outputDir) {

        Configuration hadoopConf = new Configuration();

        TemporaryFileDownloader temporaryFileDownloader = new TemporaryFileDownloader(hadoopConf);
        GridDatasetProcessor processor = new NetCdfGridDatasetProcessor(temporaryFileDownloader);
        FileNameGenerator fileNameGenerator = new SimpleFileNameGenerator(jobId);

        return new GribParquetConverter(processor, hadoopConfig, outputDir, fileNameGenerator);
    }
}
