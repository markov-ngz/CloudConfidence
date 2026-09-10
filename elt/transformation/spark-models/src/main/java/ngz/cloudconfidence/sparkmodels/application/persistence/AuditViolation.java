package ngz.cloudconfidence.sparkmodels.application.persistence;

public final class AuditViolation {

    private final String ruleName;
    private final String message;
    private final long offendingRowCount; // 0 when not applicable

    public AuditViolation(String ruleName, String message, long offendingRowCount) {
        this.ruleName = ruleName;
        this.message = message;
        this.offendingRowCount = offendingRowCount;
    }

    public AuditViolation(String ruleName, String message) {
        this(ruleName, message, 0L);
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getMessage() {
        return message;
    }

    public long getOffendingRowCount() {
        return offendingRowCount;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (offending rows: %d)", ruleName, message, offendingRowCount);
    }
}
