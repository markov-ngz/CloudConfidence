package ngz.cloudconfidence.convertgrib2.grib2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GridItem {
    private String validTime;
    private String startTime;
    private String endTime;
    private String variable;
    private String unit;
    private Double value;
    private String levelType;
    private Double levelValue;
    private int gridX;
    private int gridY;
    private boolean isLatLon;
    private String projection;
    private Double lat;
    private Double lon;
}
