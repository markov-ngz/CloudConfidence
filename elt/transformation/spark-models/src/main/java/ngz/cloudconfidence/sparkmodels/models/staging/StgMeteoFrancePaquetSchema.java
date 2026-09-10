package ngz.cloudconfidence.sparkmodels.models.staging;

import java.io.Serializable;
import lombok.Data;

@Data
public class StgMeteoFrancePaquetSchema implements Serializable {

    private String meteofrancePrevinum;
    private String meteofranceModel;
    private String meteofranceGrid;
    private String meteofrancePackage;
    private String meteofranceReferenceTime;
    private String meteofranceTime;
    private String validTime;
    private String startTime;
    private String endTime;
    private String levelType;
    private Double levelValue;
    private Boolean isLatLon;
    private String projection;
    private Integer gridX;
    private Integer gridY;
    private Double latitude;
    private Double longitude;
    private String variable;
    private String unit;
    private Double value;
    private String model;
    private Double grid;
}
