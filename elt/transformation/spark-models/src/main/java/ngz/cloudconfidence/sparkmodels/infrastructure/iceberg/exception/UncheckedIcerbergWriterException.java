package ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.exception;

public class UncheckedIcerbergWriterException extends RuntimeException {
    public UncheckedIcerbergWriterException(String message) {
        super(message);
    }

    public UncheckedIcerbergWriterException(String message, Throwable e) {
        super(message, e);
    }
}
