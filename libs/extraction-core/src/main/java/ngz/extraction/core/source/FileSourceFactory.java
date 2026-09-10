package ngz.extraction.core.source;

import java.util.Map;

public interface FileSourceFactory {
    public FileSource create(Map<String, String> props);
}
