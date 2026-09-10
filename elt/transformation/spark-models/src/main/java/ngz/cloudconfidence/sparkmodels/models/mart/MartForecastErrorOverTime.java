package ngz.cloudconfidence.sparkmodels.models.mart;

import java.io.Serializable;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class MartForecastErrorOverTime implements Serializable {
    String forecastModel;
    String stationId;
    String stationName;
    String time;
    String variable;
    String unit;
    Double avgForecastError;
    Double avgObservationValue;
    // Technical field for write date
    Timestamp techUpdatedAt;
}
