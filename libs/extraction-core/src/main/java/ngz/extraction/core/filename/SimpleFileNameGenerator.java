package ngz.extraction.core.filename;

import java.time.Instant;

/** Generate simple filename like jobId_timestamp_fileId.extension . */
public class SimpleFileNameGenerator implements FileNameGenerator {

    private final String jobId;

    public SimpleFileNameGenerator(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public String generateFileName(String fileId, String extension) {
        String cleanedFileId = fileId.replaceAll("[.,:]", "");
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        return String.format(
                "%s_%s_%s.%s", jobId, Instant.now().getEpochSecond(), cleanedFileId, extension);
    }
}
