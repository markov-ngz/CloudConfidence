package ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.exception;

public class UnsupportedStrategyException extends RuntimeException {
    public UnsupportedStrategyException(String message) {
        super(message);
    }
}
