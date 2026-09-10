package ngz.cloudconfidence.sparkmodels.infrastructure.iceberg;

import lombok.Getter;
import org.apache.iceberg.metrics.CommitReport;
import org.apache.iceberg.metrics.MetricsReport;
import org.apache.iceberg.metrics.MetricsReporter;

/** Pure from framework iceberg reporter. */
public class CustomIcebergMetricsReporter implements MetricsReporter {

    @Getter private static volatile CommitReport lastCommitReport;

    @Override
    public void report(MetricsReport report) {
        if (report instanceof CommitReport commitReport) {
            lastCommitReport = commitReport;
        }
    }

    public static void reset() {
        lastCommitReport = null;
    }
}
