package ngz.cloudconfidence.sparkmodels.models.intermediate;

import java.io.Serializable;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class IntAggForecastErrorOverTimeSchema implements Serializable {

    public String stationId;
    public String stationName;
    public String forecastModel;
    public String time;
    public String variable;
    public String unit;
    public Double sumForecastError;
    public Double sumSquaredError;
    public Double sumObservationValue;
    public Double countRecords;
    // Technical fields
    public Timestamp techWindowStartTime;
    public Timestamp techWindowEndTime;
    public Timestamp techUpdatedAt;
}
