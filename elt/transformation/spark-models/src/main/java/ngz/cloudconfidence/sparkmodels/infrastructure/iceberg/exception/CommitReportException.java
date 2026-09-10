package ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.exception;

public class CommitReportException extends RuntimeException {
    public CommitReportException(String message) {
        super(message);
    }

    public CommitReportException(String message, Throwable e) {
        super(message, e);
    }
}
