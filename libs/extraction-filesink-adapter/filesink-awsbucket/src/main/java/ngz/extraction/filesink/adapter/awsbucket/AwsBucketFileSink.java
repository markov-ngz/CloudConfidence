package ngz.extraction.filesink.adapter.awsbucket;

import ngz.extraction.core.model.FileInformation;
import ngz.extraction.core.sink.FileSink;
import ngz.extraction.core.sink.SunkFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

public class AwsBucketFileSink implements FileSink {
    private final S3Client s3client;
    private final String bucketName;
    private final String baseDirectory;

    public AwsBucketFileSink(S3Client s3client, String bucketName, String baseDirectory) {
        this.s3client = s3client;
        this.bucketName = bucketName;
        // Normalize baseDirectory: remove trailing slashes
        this.baseDirectory = baseDirectory.replaceAll("/+$", "");
    }

    @Override
    public SunkFile sink(String filename, byte[] content, FileInformation fileInformation) {

        // Normalize filename: remove leading/trailing slashes
        String normalizedFilename = filename.replaceAll("^/+|/+$", "");

        // Join paths safely
        String objectKey =
                baseDirectory.isEmpty()
                        ? normalizedFilename
                        : baseDirectory + "/" + normalizedFilename;

        s3client.putObject(
                b -> b.bucket(bucketName).key(objectKey).metadata(fileInformation.getMetadata()),
                RequestBody.fromBytes(content));

        String uri = buildS3ObjectUri(bucketName, objectKey);

        return SunkFile.builder()
                .filename(objectKey)
                .uri(uri)
                .format(fileInformation.getFormat())
                .metadata(fileInformation.getMetadata())
                .build();
    }

    protected String buildS3ObjectUri(String bucketName, String objectKey) {
        // Ensure no leading slash in objectKey for S3 URI
        String normalizedObjectKey = objectKey.replaceAll("^/+", "");
        return "s3://" + bucketName + "/" + normalizedObjectKey;
    }
}
