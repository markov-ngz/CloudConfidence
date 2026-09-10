package ngz.cloudconfidence.sparkmodels.infrastructure.iceberg;

import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.exception.IcebergWriterException;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.exception.UncheckedIcerbergWriterException;
import org.apache.iceberg.metrics.CommitReport;
import org.apache.spark.sql.DataFrameWriterV2;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for all Iceberg write operations.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Validates the {@link WriteRequest} before execution
 *   <li>Routes to the correct write path (overwrite vs SQL)
 *   <li>Manages metrics capture via {@link CustomIcebergMetricsReporter}
 *   <li>Wraps Iceberg/Spark exceptions into domain exceptions
 * </ul>
 */
public final class IcebergWriteGateway {

    private static final Logger LOG = LoggerFactory.getLogger(IcebergWriteGateway.class);

    private IcebergWriteGateway() {}

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    /**
     * Executes the write described by {@code request} and returns the Iceberg commit report.
     *
     * @throws IcebergWriterException on any write failure
     */
    public static CommitReport execute(WriteRequest request) throws IcebergWriterException {
        LOG.info(
                "Executing write — table: {}, strategy: {}",
                request.getTargetTable(),
                request.getStrategy());

        validate(request);

        CustomIcebergMetricsReporter.reset();

        try {
            route(request);
            CommitReport report = CustomIcebergMetricsReporter.getLastCommitReport();
            LOG.info("Write succeeded — table: {}", request.getTargetTable());
            return report;

        } catch (Exception e) {
            throw new IcebergWriterException(
                    "Failed to execute %s for table '%s'. Reason: %s"
                            .formatted(
                                    request.getStrategy(),
                                    request.getTargetTable(),
                                    e.getMessage()),
                    request.getTargetTable(),
                    e);
        }
    }

    // -------------------------------------------------------------------------
    // Routing
    // -------------------------------------------------------------------------

    private static void route(WriteRequest request) {
        if (request.getStrategy() == WriteStrategy.CREATE_OR_REPLACE) {
            executeOverwrite(request);
        } else {
            executeSql(request);
        }
    }

    // -------------------------------------------------------------------------
    // Write paths
    // -------------------------------------------------------------------------

    private static void executeOverwrite(WriteRequest request) {
        String targetTable =
                request.getTargetTable(); // Make sure this is just catalog.schema.table
        SparkSession spark = request.getSourceDataset().sparkSession();
        String branchName = request.getOptions().get("branch");

        LOG.info("Overwrite path — table: {}", targetTable);

        // 1. Route writes to the branch via Session Config
        if (branchName != null) {
            spark.conf().set("spark.wap.branch", branchName);
            LOG.info("Set spark.wap.branch to: {}", branchName);
        }

        try {
            DataFrameWriterV2<Row> writer = request.getSourceDataset().writeTo(targetTable);

            request.getOptions()
                    .forEach(
                            (k, v) -> {
                                // Skip passing branch as a write option since it's handled by conf
                                if (!k.equals("branch")) {
                                    LOG.info("Write option: {} = {}", k, v);
                                    writer.option(k, v);
                                }
                            });

            writer.overwritePartitions();
            LOG.info("Successfully overwrote partitions on branch: {}", branchName);
        } catch (NoSuchTableException e) {
            throw new UncheckedIcerbergWriterException(
                    "Cannot overwrite partitions — table not found: " + targetTable, e);
        }
    }

    private static void executeSql(WriteRequest request) {
        String[] columns = request.getSourceDataset().columns();

        try (TempViewManager viewManager =
                new TempViewManager(request.getSourceDataset().sparkSession())) {

            String viewName = viewManager.register(request.getSourceDataset());
            String sql =
                    SqlQueryBuilder.build(
                            request.getStrategy(),
                            request.getTargetTable(),
                            viewName,
                            request.getPrimaryKeys(),
                            columns);

            LOG.info("SQL path — executing:\n{}", sql);
            request.getSourceDataset().sparkSession().sql(sql).collect();
        }
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private static void validate(WriteRequest request) {
        if (request.getPrimaryKeys().isEmpty()
                && request.getStrategy() != WriteStrategy.APPEND
                && request.getStrategy() != WriteStrategy.CREATE_OR_REPLACE) {
            LOG.warn(
                    "No primary keys for strategy {} on table '{}' — join will be empty",
                    request.getStrategy(),
                    request.getTargetTable());
        }
    }
}
