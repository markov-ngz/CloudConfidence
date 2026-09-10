package ngz.extraction.filesink.adapter.azureblob;

import com.azure.core.credential.TokenCredential;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import java.util.Map;
import ngz.extraction.core.sink.FileSink;
import ngz.extraction.core.sink.FileSinkFactory;

public class AzureBobFileSinkFactory implements FileSinkFactory {

    public FileSink create(Map<String, String> props) {

        TokenCredential credentials = TokenCredentialFactory.create(props);

        String containerName = required(props, "containername");
        String storageAccountName = required(props, "storageaccountname");
        String blobDirectory = required(props, "blobdirectory");

        BlobServiceClient blobServiceClient =
                new BlobServiceClientBuilder()
                        .endpoint(
                                "https://"
                                        + storageAccountName
                                        + ".blob.core.windows.net/"
                                        + containerName)
                        .credential(credentials)
                        .buildClient();

        BlobContainerClient containerClient =
                blobServiceClient.getBlobContainerClient(containerName);

        return new AzureBlobFileSink(containerClient, blobDirectory);
    }

    private static String required(Map<String, String> props, String key) {
        String value = props.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return value;
    }
}
