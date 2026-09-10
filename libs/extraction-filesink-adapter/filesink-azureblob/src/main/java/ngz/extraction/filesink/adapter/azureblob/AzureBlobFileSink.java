package ngz.extraction.filesink.adapter.azureblob;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Objects;
import ngz.extraction.core.model.FileInformation;
import ngz.extraction.core.sink.FileSink;
import ngz.extraction.core.sink.SunkFile;

public class AzureBlobFileSink implements FileSink {

    private final BlobContainerClient containerClient;
    private final String blobDirectory;

    public AzureBlobFileSink(BlobContainerClient containerClient, String blobDirectory) {
        this.containerClient =
                Objects.requireNonNull(containerClient, "containerClient must not be null");

        if (blobDirectory == null || blobDirectory.isBlank()) {
            this.blobDirectory = "";
        } else {
            this.blobDirectory = blobDirectory.replaceAll("^/+|/+$", "");
        }
    }

    @Override
    public SunkFile sink(String filename, byte[] content, FileInformation fileInformation) {
        Objects.requireNonNull(filename, "filename must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(fileInformation, "fileInformation must not be null");

        if (filename.isBlank()) {
            throw new IllegalArgumentException("filename must not be blank");
        }

        if (filename.startsWith("/")) {
            throw new IllegalArgumentException("filename must be relative");
        }

        String blobFilename = blobDirectory.isEmpty() ? filename : blobDirectory + "/" + filename;

        BlobClient blobClient = containerClient.getBlobClient(blobFilename);

        blobClient.upload(new ByteArrayInputStream(content), content.length, true);

        Map<String, String> metadata = fileInformation.getMetadata();
        if (metadata != null && !metadata.isEmpty()) {
            blobClient.setMetadata(metadata);
        }

        return SunkFile.builder()
                .filename(blobFilename)
                .uri(blobClient.getBlobUrl())
                .format(fileInformation.getFormat())
                .build();
    }
}
