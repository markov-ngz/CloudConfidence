package ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.exception;

public class IcebergWriterException extends Exception {

    private final String targetTable;

    public IcebergWriterException(String message, String targetTable, Throwable cause) {
        super(String.format("Write failed on table '%s': %s", targetTable, message), cause);
        this.targetTable = targetTable;
    }

    public IcebergWriterException(String message, String targetTable) {
        this(targetTable, message, null);
    }

    public String getTargetTable() {
        return targetTable;
    }
}
