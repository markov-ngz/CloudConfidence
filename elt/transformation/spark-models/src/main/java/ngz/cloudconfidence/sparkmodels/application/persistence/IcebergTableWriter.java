package ngz.cloudconfidence.sparkmodels.application.persistence;

import java.util.List;
import java.util.Objects;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.CustomIcebergMetricsReporter;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.IcebergBranchHelper;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.IcebergWriteGateway;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.WriteRequest;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.WriteStrategy;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.exception.CommitReportException;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.exception.IcebergWriterException;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.exception.UncheckedIcerbergWriterException;
import ngz.markov.sparkmodel.execution.ModelWriteResult;
import org.apache.iceberg.metrics.CommitMetricsResult;
import org.apache.iceberg.metrics.CommitReport;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iceberg-backed implementation of {@link TableWriter}.
 *
 * <p>This is the only class allowed to import Iceberg types. {@link CommitReport} is consumed here
 * and mapped to {@link ModelWriteResult} before being returned to the application layer.
 */
public class IcebergTableWriter implements TableWriter {

    private static final Logger LOG = LoggerFactory.getLogger(IcebergTableWriter.class);

    @Override
    public ModelWriteResult append(Dataset<Row> source, String targetTable) {
        return write(
                WriteRequest.builder()
                        .sourceDataset(source)
                        .targetTable(targetTable)
                        .strategy(WriteStrategy.APPEND)
                        .build());
    }

    @Override
    public ModelWriteResult createOrReplace(Dataset<Row> source, String targetTable) {
        return write(
                WriteRequest.builder()
                        .sourceDataset(source)
                        .targetTable(targetTable)
                        .strategy(WriteStrategy.CREATE_OR_REPLACE)
                        .build());
    }

    @Override
    public ModelWriteResult insertIfNotExists(
            Dataset<Row> source, String targetTable, List<String> primaryKeys) {
        return write(
                WriteRequest.builder()
                        .sourceDataset(source)
                        .targetTable(targetTable)
                        .strategy(WriteStrategy.INSERT_IF_NOT_EXISTS)
                        .primaryKeys(primaryKeys)
                        .build());
    }

    @Override
    public ModelWriteResult merge(
            Dataset<Row> source, String targetTable, List<String> primaryKeys) {
        return write(
                WriteRequest.builder()
                        .sourceDataset(source)
                        .targetTable(targetTable)
                        .strategy(WriteStrategy.MERGE)
                        .primaryKeys(primaryKeys)
                        .build());
    }

    @Override
    public ModelWriteResult writeAndPublish(
            Dataset<Row> dataset,
            String targetTable,
            WriteStrategy strategy,
            List<String> primaryKeys,
            AuditConsumer auditConsumer) {

        SparkSession sparkSession = dataset.sparkSession();
        String catalogName = sparkSession.catalog().currentCatalog();
        String targetTableWithCatalog = String.format("%s.%s", catalogName, targetTable);
        LOG.info("Starting WAP workflow for table: {} with catalog : {}", targetTable, catalogName);
        String branchName = "wap_branch_" + System.currentTimeMillis();
        LOG.debug("Generated branch name: {}", branchName);

        try {
            // ── 1. Create branch ───────────────────────────────────────────
            LOG.info("Creating audit branch: {}", branchName);
            IcebergBranchHelper.createBranch(dataset.sparkSession(), branchName, targetTable);
            LOG.info("Successfully created audit branch: {}", branchName);

            // ── 2. Write data to branch ─────────────────────────────────────
            String targetTableWithBranch = String.format("%s.%s", targetTable, branchName);
            String targetTableWithBranchAndCatalog =
                    String.format("%s.%s", targetTableWithCatalog, branchName);
            LOG.debug("Target table with branch: {}", targetTableWithBranch);

            LOG.info("Executing write operation for table: {}", targetTableWithBranch);

            WriteRequest writeRequest =
                    WriteRequest.builder()
                            .sourceDataset(dataset)
                            .primaryKeys(primaryKeys)
                            .targetTable(targetTableWithCatalog)
                            .strategy(strategy)
                            .branch(branchName)
                            .build();
            CommitReport commitReport = IcebergWriteGateway.execute(writeRequest);
            LOG.info("Write operation completed. Commit report: {}", commitReport);

            // ── 3. Audit ─────────────────────────────────────────────────────
            LOG.info("Loading audit table: {}", targetTableWithBranch);
            Dataset<Row> auditTable =
                    sparkSession
                            .read()
                            .format("iceberg")
                            .option("branch", branchName)
                            .load(targetTableWithCatalog);

            LOG.info("Running audit checks for table: {}", targetTableWithBranch);
            auditConsumer.audit(auditTable);
            LOG.info("Audit checks passed for table: {}", targetTableWithBranch);

            // ── 4. Merge branch into main ───────────────────────────────────
            LOG.info("Merging branch '{}' into 'main' for table: {}", branchName, targetTable);
            IcebergBranchHelper.mergeBranch(
                    sparkSession, catalogName, branchName, targetTableWithCatalog, "main");
            LOG.info(
                    "Successfully merged branch '{}' into 'main' for table: {}",
                    branchName,
                    targetTable);

            IcebergBranchHelper.dropBranch(sparkSession, targetTable, branchName, true);

            return toModelWriteResult(commitReport);

        } catch (AuditException e) {
            LOG.error(
                    "Audit failed for table: {}. Branch '{}' will be dropped. Error: {}",
                    targetTable,
                    branchName,
                    e.getMessage(),
                    e);
            IcebergBranchHelper.dropBranch(sparkSession, targetTable, branchName, true);

            throw new UncheckedIcerbergWriterException("Audit failure", e);
        } catch (IcebergWriterException e) {
            LOG.error(
                    "Write operation failed for table: {}. Branch '{}' will be dropped. Error: {}",
                    targetTable,
                    branchName,
                    e.getMessage(),
                    e);
            IcebergBranchHelper.dropBranch(sparkSession, targetTable, branchName, true);
            throw new UncheckedIcerbergWriterException("", e);
        } catch (Exception e) {
            LOG.error(
                    "Unexpected error in WAP workflow for table: {}. Branch '{}' will be dropped. Error: {}",
                    targetTable,
                    branchName,
                    e.getMessage(),
                    e);
            IcebergBranchHelper.dropBranch(sparkSession, targetTable, branchName, true);
            throw e;
        }
    }

    @Override
    public ModelWriteResult write(WriteRequest request) {
        LOG.info(
                "write -> table: {}, strategy: {}",
                request.getTargetTable(),
                request.getStrategy());

        CustomIcebergMetricsReporter.reset();
        try {
            IcebergWriteGateway.execute(request);
            // CommitReport is Iceberg-only — mapped here, never returned raw
            CommitReport report = CustomIcebergMetricsReporter.getLastCommitReport();
            return toModelWriteResult(report);
        } catch (Exception e) {
            LOG.error(
                    "Write failed for table '{}': {}", request.getTargetTable(), e.getMessage(), e);
            throw new UncheckedIcerbergWriterException(
                    "Failed to execute %s for table '%s'. Reason: %s"
                            .formatted(
                                    request.getStrategy(),
                                    request.getTargetTable(),
                                    e.getMessage()),
                    e);
        }
    }

    // -------------------------------------------------------------------------
    // Private — Iceberg specifics, CommitReport stays in this layer
    // -------------------------------------------------------------------------

    /**
     * Maps an Iceberg {@link CommitReport} to the domain {@link ModelWriteResult}. This is the
     * single translation point — CommitReport never crosses this boundary.
     */
    private static ModelWriteResult toModelWriteResult(CommitReport commitReport) {
        if (commitReport == null) {
            throw new CommitReportException(
                    "No Iceberg CommitReport was captured for the write operation.");
        }

        try {
            CommitMetricsResult metrics = commitReport.commitMetrics();

            long recordsWritten = Objects.requireNonNull(metrics.addedRecords()).value();

            long filesWritten = Objects.requireNonNull(metrics.addedDataFiles()).value();

            long bytesWritten = Objects.requireNonNull(metrics.addedFilesSizeInBytes()).value();

            return new ModelWriteResult(recordsWritten, filesWritten, bytesWritten, true);

        } catch (Exception e) {
            throw new CommitReportException(
                    "Failed to convert Iceberg CommitReport to WriteResult.", e);
        }
    }

    /**
     * Publishes a WAP branch to main using Iceberg's stored procedure. Pure Iceberg concern —
     * invisible to the interface.
     */
    private void publishBranch(SparkSession spark, String targetTable, String branch) {
        String sql =
                "CALL iceberg.system.publish_changes('%s', '%s')".formatted(targetTable, branch);
        LOG.debug("Publishing WAP branch with: {}", sql);
        spark.sql(sql);
    }
}
