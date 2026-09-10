package ngz.cloudconfidence.sparkmodels.models.source;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import lombok.Data;
import ngz.markov.sparkmodel.execution.DependencyResolver;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.model.Layer;
import ngz.markov.sparkmodel.model.Materialization;
import ngz.markov.sparkmodel.model.SparkColumn;
import ngz.markov.sparkmodel.model.SparkModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public class SourceMeteoFranceObservation extends SparkModel {

    private SourceMeteoFranceObservation(SourceMeteoFranceObservation.Builder builder) {

        super(builder);
    }

    public static SourceMeteoFranceObservation.Builder builder() {
        return new SourceMeteoFranceObservation.Builder();
    }

    public static class Builder extends SparkModel.Builder<SourceMeteoFranceObservation> {
        @Override
        public SourceMeteoFranceObservation build() {
            return new SourceMeteoFranceObservation(this);
        }
    }

    @Override
    protected Layer defaultNodeType() {
        return Layer.SOURCE;
    }

    @Override
    protected List<String> defaultPrimaryKey() {
        return List.of();
    }

    @Override
    protected Materialization defaultMaterialization() {
        return null;
    }

    @Override
    public Class<?> schemaClass() {
        return SourceMeteoFranceObservation.Schema.class; // direct, no wrapping
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {
        return read(pipelineContext);
    }

    @Data
    public static class Schema implements Serializable {

        @SparkColumn(name = "lat", nullable = false)
        private Double latitude;

        @SparkColumn(name = "lon", nullable = false)
        private Double longitude;

        @SparkColumn(name = "geo_id_insee", nullable = false)
        private String inseeLocationId;

        @SparkColumn(name = "reference_time", nullable = false)
        private Timestamp productionTime;

        @SparkColumn(name = "insert_time", nullable = false)
        private Timestamp ingestionTime;

        @SparkColumn(name = "validity_time", nullable = false)
        private Timestamp observationTime;

        @SparkColumn(name = "t")
        private Double airTemperature;

        @SparkColumn(name = "td")
        private Double dewPointTemperature;

        @SparkColumn(name = "tx")
        private Double maxAirTemperature;

        @SparkColumn(name = "tn")
        private Double minAirTemperature;

        @SparkColumn(name = "u")
        private Double relativeHumidity;

        @SparkColumn(name = "ux")
        private Double maxRelativeHumidity;

        @SparkColumn(name = "un")
        private Double minRelativeHumidity;

        @SparkColumn(name = "dd")
        private Double windDirection;

        @SparkColumn(name = "ff")
        private Double windSpeed;

        @SparkColumn(name = "dxy")
        private Double maxWindDirection;

        @SparkColumn(name = "fxy")
        private Double maxWindSpeed;

        @SparkColumn(name = "ddraf")
        private Double gustDirection;

        @SparkColumn(name = "raf")
        private Double maxWindGust;

        @SparkColumn(name = "rr1")
        private Double hourlyPrecipitation;

        @SparkColumn(name = "t_10")
        private Double soilTemperature10cm;

        @SparkColumn(name = "t_20")
        private Double soilTemperature20cm;

        @SparkColumn(name = "t_50")
        private Double soilTemperature50cm;

        @SparkColumn(name = "t_100")
        private Double soilTemperature100cm;

        @SparkColumn(name = "vv")
        private Double horizontalVisibility;

        @SparkColumn(name = "etat_sol")
        private Integer groundStateCode;

        @SparkColumn(name = "sss")
        private Double snowDepth;

        @SparkColumn(name = "n")
        private Double totalCloudCover;

        @SparkColumn(name = "insolh")
        private Double sunshineDuration;

        @SparkColumn(name = "ray_glo01")
        private Double hourlyGlobalRadiation;

        @SparkColumn(name = "pres")
        private Double stationPressure;

        @SparkColumn(name = "pmer")
        private Double seaLevelPressure;
    }
}
