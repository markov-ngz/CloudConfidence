package ngz.cloudconfidence.sparkmodels.infrastructure.iceberg;

import java.util.UUID;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TempViewManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TempViewManager.class);

    private final SparkSession spark;
    private final String viewName;

    public TempViewManager(SparkSession spark) {
        this.spark = spark;
        this.viewName = "source_" + UUID.randomUUID().toString().replace("-", "");
        LOG.debug("Allocated temp view name: {}", viewName);
    }

    public String register(Dataset<Row> dataset) {
        dataset.createOrReplaceTempView(viewName);
        LOG.info("Registered temp view: {}", viewName);
        return viewName;
    }

    public String getViewName() {
        return viewName;
    }

    @Override
    public void close() {
        try {
            spark.catalog().dropTempView(viewName);
            LOG.debug("Dropped temp view: {}", viewName);
        } catch (Exception e) {
            LOG.warn("Failed to drop temp view '{}': {}", viewName, e.getMessage());
        }
    }
}
