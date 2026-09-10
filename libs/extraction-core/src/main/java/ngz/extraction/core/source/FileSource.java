package ngz.extraction.core.source;

import java.util.List;
import ngz.extraction.core.model.FileInformation;

public interface FileSource {
    List<FileInformation> listFiles();

    byte[] downloadFile(String fileLocation);
}
