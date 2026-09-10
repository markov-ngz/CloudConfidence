package ngz.cloudconfidence.elt.extraction;

import java.util.List;
import ngz.cloudconfidence.elt.extraction.airflow.XcomUtils;
import ngz.cloudconfidence.elt.extraction.exception.FileExtractionException;
import ngz.extraction.core.filename.FileNameGenerator;
import ngz.extraction.core.model.FileInformation;
import ngz.extraction.core.sink.FileSink;
import ngz.extraction.core.sink.SunkFile;
import ngz.extraction.core.source.FileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileExtractionEngine {
    private static final Logger LOG = LoggerFactory.getLogger(FileExtractionEngine.class);

    private final FileSource fileSource;
    private final FileSink fileSink;
    private final FileNameGenerator fileNameGenerator;

    // Dependencies are injected via constructor
    public FileExtractionEngine(
            FileSource fileSource, FileSink fileSink, FileNameGenerator fileNameGenerator) {
        this.fileSource = fileSource;
        this.fileSink = fileSink;
        this.fileNameGenerator = fileNameGenerator;
    }

    public ExtractionResult execute() throws FileExtractionException {
        ExtractionResult extractionResult = new ExtractionResult();

        LOG.info("Beginning listing files");

        List<FileInformation> fileInfos = fileSource.listFiles();

        LOG.info("Successfully listing " + fileInfos.size() + " files");
        LOG.info("Files listed :" + fileInfos);

        for (FileInformation fileInfo : fileInfos) {

            LOG.info("Begin processing file :" + fileInfo.getLocation());

            ProcessResult result = processSingleFile(fileInfo);

            if (result.isSuccess()) {
                extractionResult.addSuccess(result.getSunkFile());
            } else {
                LOG.error(
                        "Failed to extract fileInformation: {}",
                        fileInfo.getId(),
                        result.getException());
                extractionResult.addFailure(fileInfo, result.getException());
            }

            LOG.info("End processing file :" + fileInfo.getLocation());
        }

        if (!extractionResult.getFailures().isEmpty()) {

            LOG.warn(
                    "Errors occurred during extraction , detected "
                            + extractionResult.getFailures().size()
                            + " errors.");

            XcomUtils.pushXcom(extractionResult, false);

            LOG.warn("Succesfully pushed Xcom, throwing error ..");

            throw new FileExtractionException(
                    "Batch processing completed with "
                            + extractionResult.getFailures().size()
                            + " failures.",
                    extractionResult.getFailures());
        }

        XcomUtils.pushXcom(extractionResult, true);

        LOG.info("Succesfully pushed Xcom");

        return extractionResult;
    }

    private ProcessResult processSingleFile(FileInformation fileInfo) {
        try {
            byte[] data = fileSource.downloadFile(fileInfo.getLocation());
            String filename =
                    fileNameGenerator.generateFileName(fileInfo.getId(), fileInfo.getFormat());
            SunkFile sunkFile = fileSink.sink(filename, data, fileInfo);
            return ProcessResult.success(sunkFile);
        } catch (Exception e) {
            return ProcessResult.failure(e);
        }
    }
}
