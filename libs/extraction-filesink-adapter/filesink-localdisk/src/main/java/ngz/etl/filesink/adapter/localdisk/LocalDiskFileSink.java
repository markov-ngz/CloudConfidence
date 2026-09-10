package ngz.etl.filesink.adapter.localdisk;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import ngz.extraction.core.exception.WriteFileExtractionException;
import ngz.extraction.core.file.FileInformation;
import ngz.extraction.core.file.sink.FileSink;
import ngz.extraction.core.file.sink.SunkFile;

public class LocalDiskFileSink implements FileSink {

    private final Path outputFolder;
    private final ChecksumCompute checksumCompute ;


    public LocalDiskFileSink(Path outputFolder, String checksumAlgorithm) {
        this.checksumCompute = new ChecksumCompute(checksumAlgorithm);
        this.outputFolder = outputFolder;
    }

    @Override
    public SunkFile sink(String filename, byte[] content, FileInformation info) {
        Path outputPath = outputFolder.resolve(filename);
        try {
            Files.write(outputPath, content);
            return SunkFile.builder()
                    .uri(outputPath.toAbsolutePath().toString())
                    .format(info.getFormat())
                    .build();

        } catch (IOException e) {
            throw new WriteFileExtractionException(
                    "Failed to write file to target folder at location :" + outputPath.toString(),
                    e);
        }
    }


}
