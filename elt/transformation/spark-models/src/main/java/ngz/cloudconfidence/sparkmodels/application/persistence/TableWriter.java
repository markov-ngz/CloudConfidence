package ngz.cloudconfidence.sparkmodels.application.persistence;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.WriteRequest;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.WriteStrategy;
import ngz.markov.sparkmodel.execution.ModelWriteResult;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * Domain contract for writing a Spark dataset to a table.
 *
 * <p>Implementations are pure infrastructure concerns (Iceberg, Delta Lake, JDBC...). No
 * infrastructure type crosses this boundary — inputs and outputs are domain objects only.
 */
public interface TableWriter {

    /** Appends rows to the target table without any deduplication. */
    ModelWriteResult append(Dataset<Row> source, String targetTable);

    /** Fully replaces the target table content atomically. */
    ModelWriteResult createOrReplace(Dataset<Row> source, String targetTable);

    /**
     * Inserts only rows that do not match an existing record by primary key. Existing rows are left
     * untouched.
     */
    ModelWriteResult insertIfNotExists(
            Dataset<Row> source, String targetTable, List<String> primaryKeys);

    /** Full upsert: updates matched rows, inserts unmatched ones. */
    ModelWriteResult merge(Dataset<Row> source, String targetTable, List<String> primaryKeys);

    /**
     * Write-Audit-Publish workflow: writes to a staging branch, runs the provided audit, then
     * publishes to main only if the audit passes.
     *
     * @param auditConsumer called with the staged result before publish; should throw if the audit
     *     fails to abort the publish.
     */
    ModelWriteResult writeAndPublish(
            Dataset<Row> source,
            String targetTable,
            WriteStrategy strategy,
            List<String> primaryKeys,
            AuditConsumer auditConsumer);

    /**
     * Escape hatch for callers needing fine-grained control (branch targeting, custom options,
     * etc.). Prefer the typed methods above whenever possible.
     */
    ModelWriteResult write(WriteRequest request);
}
