package ngz.etl.filesink.adapter.localdisk;

import java.nio.file.Path;
import java.util.Map;
import ngz.extraction.core.file.sink.FileSink;
import ngz.extraction.core.file.sink.FileSinkFactory;

public class LocalDiskFileSinkFactory implements FileSinkFactory {

    private String checksumAlgorithm = "SHA-512";

    public FileSink create(Map<String, String> props) {

        if (!props.containsKey("outputdir")) {
            throw new IllegalArgumentException("Missing property 'outputdir' from props argument");
        }

        String outputDirPath = props.get("outputdir");
        Path outputDir = Path.of(outputDirPath);

        return new LocalDiskFileSink(outputDir,checksumAlgorithm);
    }
}
