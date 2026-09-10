package ngz.cloudconfidence.sparkmodels.application.persistence;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

@FunctionalInterface
public interface AuditConsumer {
    /**
     * Receives the branch view of the table after writing. Implementors throw AuditException to
     * signal a failed check. Multiple checks can be accumulated via AuditException#addViolation().
     *
     * @param branchTable The Dataset pointing at the WAP branch (read-only)
     * @throws AuditException if one or more data-quality rules fail
     */
    void audit(Dataset<Row> branchTable) throws AuditException;
}
