package ngz.cloudconfidence.sparkmodels.models.intermediate;

import java.io.Serializable;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class IntUnpivotObservationSchema implements Serializable {

    private String variable;
    private Double latitude;
    private Double longitude;
    private String stationId;
    private String stationName;
    private Timestamp observationTime;
    private Double value;
    private String unit;
}
