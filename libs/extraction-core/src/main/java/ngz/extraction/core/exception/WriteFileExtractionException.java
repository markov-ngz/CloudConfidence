package ngz.extraction.core.exception;

public class WriteFileExtractionException extends FileExtractionException {
    public WriteFileExtractionException(String message) {
        super(message);
    }

    public WriteFileExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public WriteFileExtractionException(Throwable cause) {
        super(cause);
    }
}
