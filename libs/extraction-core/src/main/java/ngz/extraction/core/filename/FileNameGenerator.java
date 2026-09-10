package ngz.extraction.core.filename;

/** Generate a fileName for a given file id. */
public interface FileNameGenerator {
    public String generateFileName(String fileId, String extension);
}
