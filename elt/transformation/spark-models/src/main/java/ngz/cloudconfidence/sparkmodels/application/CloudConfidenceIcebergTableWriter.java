package ngz.cloudconfidence.sparkmodels.application;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.application.persistence.AuditConsumer;
import ngz.cloudconfidence.sparkmodels.application.persistence.TableWriter;
import ngz.cloudconfidence.sparkmodels.application.persistence.TableWriterRegistry;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.WriteStrategy;
import ngz.markov.sparkmodel.execution.ModelWriteResult;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static facade over {@link TableWriter}.
 *
 * <p>Call sites remain unchanged (static calls, no injection needed). The actual implementation is
 * resolved through {@link TableWriterRegistry}, which is configured once at application startup.
 *
 * <p>To swap implementations (tests, Delta migration, etc.), register a different {@link
 * TableWriter} — this class never changes.
 */
public final class CloudConfidenceIcebergTableWriter {

    private static final Logger LOG =
            LoggerFactory.getLogger(CloudConfidenceIcebergTableWriter.class);

    private CloudConfidenceIcebergTableWriter() {}

    // -------------------------------------------------------------------------
    // Public API — mirrors TableWriter, static for ergonomics
    // -------------------------------------------------------------------------

    public static ModelWriteResult append(Dataset<Row> dataset, String targetTable) {
        LOG.info("append -> table: {}", targetTable);
        return TableWriterRegistry.get().append(dataset, targetTable);
    }

    public static ModelWriteResult createOrReplaceTable(Dataset<Row> dataset, String targetTable) {
        LOG.info("createOrReplace -> table: {}", targetTable);
        return TableWriterRegistry.get().createOrReplace(dataset, targetTable);
    }

    public static ModelWriteResult createOrReplaceTable(
            Dataset<Row> dataset, String targetTable, AuditConsumer auditConsumer) {
        LOG.info("createOrReplace (WAP) -> table: {}", targetTable);
        return TableWriterRegistry.get()
                .writeAndPublish(
                        dataset,
                        targetTable,
                        WriteStrategy.CREATE_OR_REPLACE,
                        List.of(),
                        auditConsumer);
    }

    public static ModelWriteResult insertIfNotExists(
            Dataset<Row> dataset, String targetTable, List<String> primaryKeys) {
        LOG.info("insertIfNotExists -> table: {}, keys: {}", targetTable, primaryKeys);
        return TableWriterRegistry.get().insertIfNotExists(dataset, targetTable, primaryKeys);
    }

    public static ModelWriteResult merge(
            Dataset<Row> dataset, String targetTable, List<String> primaryKeys) {
        LOG.info("merge -> table: {}, keys: {}", targetTable, primaryKeys);
        return TableWriterRegistry.get().merge(dataset, targetTable, primaryKeys);
    }
}
