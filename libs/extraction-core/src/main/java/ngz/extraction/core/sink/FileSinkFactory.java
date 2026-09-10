package ngz.extraction.core.sink;

import java.util.Map;

public interface FileSinkFactory {
    public FileSink create(Map<String, String> props);
}
