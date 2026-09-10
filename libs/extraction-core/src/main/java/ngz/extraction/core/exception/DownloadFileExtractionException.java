package ngz.extraction.core.exception;

public class DownloadFileExtractionException extends FileExtractionException {
    public DownloadFileExtractionException(String message) {
        super(message);
    }

    public DownloadFileExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadFileExtractionException(Throwable cause) {
        super(cause);
    }
}
