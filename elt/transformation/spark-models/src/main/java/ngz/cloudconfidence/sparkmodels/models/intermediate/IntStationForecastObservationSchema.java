package ngz.cloudconfidence.sparkmodels.models.intermediate;

import java.io.Serializable;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class IntStationForecastObservationSchema implements Serializable {

    String forecastModel;
    String stationId;
    String stationName;
    Double stationLatitude;
    Double stationLongitude;
    String referenceTime;
    String time;
    Timestamp observationTime;
    String variable;
    Double forecastValue;
    String forecastUnit;
    Double forecastError;
    Double observationValue;
    String observationUnit;
}
