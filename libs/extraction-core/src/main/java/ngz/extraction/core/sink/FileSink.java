package ngz.extraction.core.sink;

import ngz.extraction.core.model.FileInformation;

public interface FileSink {
    SunkFile sink(String fileName, byte[] content, FileInformation info);
}
