package ngz.cloudconfidence.sparkmodels.infrastructure.iceberg;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcebergBranchHelper {

    private static final Logger LOG = LoggerFactory.getLogger(IcebergBranchHelper.class);

    /**
     * Creates a branch on the target Iceberg table.
     *
     * @param spark Active SparkSession
     * @param branchName Name of the branch to create
     * @param targetTable Fully qualified table name (e.g. "prod.db.sample")
     * @return The branch name that was created
     */
    public static String createBranch(SparkSession spark, String branchName, String targetTable) {
        LOG.info("Creating branch '{}' on table '{}'", branchName, targetTable);

        String currentCatalog = spark.catalog().currentCatalog();
        LOG.debug("Current catalog: {}", currentCatalog);

        String createBranchStatement =
                String.format(
                        "ALTER TABLE %s.%s CREATE BRANCH IF NOT EXISTS `%s`",
                        currentCatalog, targetTable, branchName);
        LOG.info("Executing SQL: {}", createBranchStatement);

        try {
            spark.sql(createBranchStatement);
            LOG.info("Successfully created branch '{}' on table '{}'", branchName, targetTable);
            return branchName;
        } catch (Exception e) {
            LOG.error(
                    "Failed to create branch '{}' on table '{}'. Error: {}",
                    branchName,
                    targetTable,
                    e.getMessage(),
                    e);
            throw new RuntimeException(
                    String.format(
                            "Failed to create branch '%s' on table '%s'. Reason: %s",
                            branchName, targetTable, e.getMessage()),
                    e);
        }
    }

    /**
     * Drops a branch from the target Iceberg table.
     *
     * @param spark Active SparkSession
     * @param targetTable Fully qualified table name
     * @param branchName Name of the branch to drop
     * @param quietly If true, suppress exceptions and log a warning instead
     */
    public static void dropBranch(
            SparkSession spark, String targetTable, String branchName, boolean quietly) {

        String currentCatalog = spark.catalog().currentCatalog();
        LOG.debug("Current catalog: {}", currentCatalog);

        LOG.info(
                "Dropping branch '{}' from table '{}' (quietly={})",
                branchName,
                targetTable,
                quietly);
        String dropBranchStatement =
                String.format(
                        "ALTER TABLE %s.%s DROP BRANCH IF EXISTS `%s`",
                        currentCatalog, targetTable, branchName);
        LOG.info("Executing SQL: {}", dropBranchStatement);

        try {
            spark.sql(dropBranchStatement);
            LOG.info("Successfully dropped branch '{}' from table '{}'", branchName, targetTable);
        } catch (Exception ex) {
            if (quietly) {
                LOG.warn(
                        "Could not drop branch '{}' on '{}'. Error: {}",
                        branchName,
                        targetTable,
                        ex.getMessage(),
                        ex);
            } else {
                LOG.error(
                        "Failed to drop branch '{}' on '{}'. Error: {}",
                        branchName,
                        targetTable,
                        ex.getMessage(),
                        ex);
                throw ex;
            }
        }
    }

    /**
     * Fast-forward merges a branch into main (or another target branch). Iceberg supports
     * fast-forward merge only: the branch must be ahead of main and share the same history.
     *
     * @param spark Active SparkSession
     * @param sourceBranch The branch to merge FROM (e.g. "audit-branch")
     * @param targetTable Fully qualified table name (e.g. "prod.db.sample")
     * @param targetBranch The branch to merge INTO (typically "main")
     */
    public static void mergeBranch(
            SparkSession spark,
            String catalogName,
            String sourceBranch,
            String targetTable,
            String targetBranch) {
        LOG.info(
                "Merging branch '{}' into '{}' on table '{}'",
                sourceBranch,
                targetBranch,
                targetTable);
        String fastForwardStatement =
                String.format(
                        "CALL %s.system.fast_forward('%s', '%s', '%s')",
                        catalogName, targetTable, targetBranch, sourceBranch);
        LOG.info("Executing SQL: {}", fastForwardStatement);

        try {
            spark.sql(fastForwardStatement);
            LOG.info(
                    "Successfully merged branch '{}' into '{}' on table '{}'",
                    sourceBranch,
                    targetBranch,
                    targetTable);
        } catch (Exception e) {
            LOG.error(
                    "Failed to merge branch '{}' into '{}' on table '{}'. Error: {}",
                    sourceBranch,
                    targetBranch,
                    targetTable,
                    e.getMessage(),
                    e);
            throw new RuntimeException(
                    String.format(
                            "Failed to merge branch '%s' into '%s' on table '%s'. Reason: %s",
                            sourceBranch, targetBranch, targetTable, e.getMessage()),
                    e);
        }
    }
}
