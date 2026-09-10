package ngz.cloudconfidence.sparkmodels.models.seed;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;
import lombok.Data;
import ngz.markov.sparkmodel.execution.DependencyResolver;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.model.Layer;
import ngz.markov.sparkmodel.model.Materialization;
import ngz.markov.sparkmodel.model.SparkModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public class SeedMeteoFranceStation extends SparkModel {

    private SeedMeteoFranceStation(SeedMeteoFranceStation.Builder builder) {

        super(builder);
    }

    public static SeedMeteoFranceStation.Builder builder() {
        return new SeedMeteoFranceStation.Builder();
    }

    public static class Builder extends SparkModel.Builder<SeedMeteoFranceStation> {
        @Override
        public SeedMeteoFranceStation build() {
            return new SeedMeteoFranceStation(this);
        }
    }

    @Override
    protected Layer defaultNodeType() {
        return Layer.SEED;
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
        return SeedMeteoFranceStation.Schema.class; // direct, no wrapping
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {
        return read(pipelineContext);
    }

    @Data
    public static class Schema implements Serializable {

        String stationId;
        String stationOmmId;
        String stationName;
        Double latitude;
        Double longitude;
        Double altitude;
        Date openingDate;
        String pack;
    }
}
