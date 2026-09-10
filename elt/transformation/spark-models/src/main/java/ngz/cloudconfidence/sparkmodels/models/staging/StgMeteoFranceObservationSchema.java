package ngz.cloudconfidence.sparkmodels.models.staging;

import java.io.Serializable;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class StgMeteoFranceObservationSchema implements Serializable {

    private Double latitude;

    private Double longitude;

    private String stationId;

    private Timestamp productionTime;

    private Timestamp ingestionTime;
    private Timestamp observationTime;

    private Double airTemperature;
    private Double dewPointTemperature;
    private Double maxAirTemperature;
    private Double minAirTemperature;

    // All measurements are Double except groundStateCode
    private Double relativeHumidity;
    private Double maxRelativeHumidity;
    private Double minRelativeHumidity;

    private Double windDirection;
    private Double windSpeed;

    private Double maxWindDirection;
    private Double maxWindSpeed;

    private Double gustDirection;
    private Double maxWindGust;

    private Double hourlyPrecipitation;

    private Double soilTemperature10cm;
    private Double soilTemperature20cm;
    private Double soilTemperature50cm;
    private Double soilTemperature100cm;

    private Integer groundStateCode;

    private Double snowDepth;

    private Double horizontalVisibility;
    private Double totalCloudCover;
    private Double sunshineDuration;
    private Double hourlyGlobalRadiation;

    private Double stationPressure;
    private Double seaLevelPressure;

    private String stationName;
}
