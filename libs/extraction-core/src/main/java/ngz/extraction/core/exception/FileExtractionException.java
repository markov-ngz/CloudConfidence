package ngz.extraction.core.exception;

public abstract class FileExtractionException extends RuntimeException {

    public FileExtractionException(String message) {
        super(message);
    }

    public FileExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileExtractionException(Throwable cause) {
        super(cause);
    }
}
