package ngz.cloudconfidence.sparkmodels.models.intermediate;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedMeteoFranceStation;
import ngz.cloudconfidence.sparkmodels.models.staging.StgMeteoFrancePaquetModel;
import ngz.cloudconfidence.sparkmodels.models.staging.StgMeteoFrancePaquetSchema;
import ngz.markov.sparkmodel.execution.DependencyResolver;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.model.Layer;
import ngz.markov.sparkmodel.model.Materialization;
import ngz.markov.sparkmodel.model.SparkModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

public class IntStationGridMappingModel extends SparkModel {

    public static final List<Class<? extends SparkModel>> DEPENDENCIES =
            List.of(SeedMeteoFranceStation.class, StgMeteoFrancePaquetModel.class);

    private IntStationGridMappingModel(IntStationGridMappingModel.Builder builder) {

        super(builder);
    }

    @Override
    protected Layer defaultNodeType() {
        return Layer.INTERMEDIATE;
    }

    @Override
    protected List<Class<? extends SparkModel>> defaultDeps() {
        return DEPENDENCIES;
    }

    @Override
    protected List<String> defaultPrimaryKey() {
        return List.of();
    }

    @Override
    protected Materialization defaultMaterialization() {
        return Materialization.EPHEMERAL;
    }

    @Override
    public Class<?> schemaClass() {
        return IntStationGridMappingSchema.class;
    }

    // --- Static factory method ---
    public static IntStationGridMappingModel.Builder builder() {
        return new IntStationGridMappingModel.Builder();
    }

    public static class Builder extends SparkModel.Builder<IntStationGridMappingModel> {
        @Override
        public IntStationGridMappingModel build() {
            return new IntStationGridMappingModel(this);
        }
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {

        Dataset<StgMeteoFrancePaquetSchema> stgMeteoFrancePaquet =
                upstream.get(StgMeteoFrancePaquetModel.class)
                        .as(Encoders.bean(StgMeteoFrancePaquetSchema.class));

        Dataset<SeedMeteoFranceStation.Schema> meteofranceStations =
                upstream.get(SeedMeteoFranceStation.class)
                        .as(Encoders.bean(SeedMeteoFranceStation.Schema.class));

        return IntStationGridMappingProcessor.process(
                        pipelineContext.lookBackWindow(), stgMeteoFrancePaquet, meteofranceStations)
                .toDF();
    }
}
