package ngz.extraction.filesink.adapter.awsbucket;

import java.util.Map;
import ngz.extraction.core.sink.FileSink;
import ngz.extraction.core.sink.FileSinkFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

public class AwsBucketFileSinkFactory implements FileSinkFactory {
    @Override
    public FileSink create(Map<String, String> props) {

        String bucketName = required(props, "bucket");
        String baseDirectory = required(props, "basedirectory");

        S3Client s3Client =
                S3Client.builder()
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .build();

        return new AwsBucketFileSink(s3Client, bucketName, baseDirectory);
    }

    private static String required(Map<String, String> props, String key) {
        String value = props.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return value;
    }
}
