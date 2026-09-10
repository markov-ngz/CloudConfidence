package ngz.cloudconfidence.sparkmodels.models.intermediate;

import java.io.Serializable;
import lombok.Data;

@Data
public class IntStationGridMappingSchema implements Serializable {

    String stationId;
    String stationName;
    Double stationLatitude;
    Double stationLongitude;
    String model;
    Double grid;
    Double gridLatitude;
    Double gridLongitude;
    Double boxDeg;
}
