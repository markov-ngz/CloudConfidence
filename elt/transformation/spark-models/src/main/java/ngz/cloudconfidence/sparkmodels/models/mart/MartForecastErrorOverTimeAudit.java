package ngz.cloudconfidence.sparkmodels.models.mart;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.trim;

import java.util.ArrayList;
import java.util.List;
import ngz.cloudconfidence.sparkmodels.application.persistence.AuditConsumer;
import ngz.cloudconfidence.sparkmodels.application.persistence.AuditException;
import ngz.cloudconfidence.sparkmodels.application.persistence.AuditViolation;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MartForecastErrorOverTimeAudit implements AuditConsumer {

    public static final String[] COLUMNS_TO_CHECK = {
        "forecastModel", "stationId", "stationName", "time", "variable", "unit"
    };

    private static final Logger LOG = LoggerFactory.getLogger(MartForecastErrorOverTimeAudit.class);

    @Override
    public void audit(Dataset<Row> branchTable) throws AuditException {

        LOG.info("Begin audit check for model MartForecastErrorOverTime");
        List<AuditViolation> violations = new ArrayList<>();

        for (String column : COLUMNS_TO_CHECK) {
            long emptyCount =
                    branchTable
                            .filter(col(column).isNull().or(trim(col(column)).equalTo("")))
                            .count();

            if (emptyCount > 0) {
                violations.add(
                        new AuditViolation(
                                "EmptyOrNullColumnCheck",
                                "Column '" + column + "' contains empty or null values.",
                                emptyCount));
            }
        }

        if (!violations.isEmpty()) {
            LOG.error("Audit violations detected for MartForecastErrorOverTime , throwing error ");
            throw new AuditException(violations);
        }

        LOG.info("No audit violation detected for MartForecastErrorOverTime");
    }
}
