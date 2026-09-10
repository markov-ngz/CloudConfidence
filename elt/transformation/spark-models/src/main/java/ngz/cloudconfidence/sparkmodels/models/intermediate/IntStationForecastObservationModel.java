package ngz.cloudconfidence.sparkmodels.models.intermediate;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.application.CloudConfidenceIcebergTableWriter;
import ngz.cloudconfidence.sparkmodels.models.staging.StgMeteoFrancePaquetModel;
import ngz.cloudconfidence.sparkmodels.models.staging.StgMeteoFrancePaquetSchema;
import ngz.markov.sparkmodel.execution.DependencyResolver;
import ngz.markov.sparkmodel.execution.ModelWriteResult;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.model.Layer;
import ngz.markov.sparkmodel.model.Materialization;
import ngz.markov.sparkmodel.model.SparkModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

public class IntStationForecastObservationModel extends SparkModel {

    public static final List<Class<? extends SparkModel>> DEPENDENCIES =
            List.of(
                    StgMeteoFrancePaquetModel.class,
                    IntStationGridMappingModel.class,
                    IntUnpivotObservationModel.class);

    private IntStationForecastObservationModel(IntStationForecastObservationModel.Builder builder) {

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
        return List.of("forecastModel", "stationId", "referenceTime", "time", "variable");
    }

    @Override
    protected Materialization defaultMaterialization() {
        return Materialization.EPHEMERAL;
    }

    @Override
    public Class<?> schemaClass() {
        return IntStationForecastObservationSchema.class;
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {

        Dataset<StgMeteoFrancePaquetSchema> stgMeteoFrancePaquet =
                upstream.get(StgMeteoFrancePaquetModel.class)
                        .as(Encoders.bean(StgMeteoFrancePaquetSchema.class));

        Dataset<IntStationGridMappingSchema> intStationGridMapping =
                upstream.get(IntStationGridMappingModel.class)
                        .as(Encoders.bean(IntStationGridMappingSchema.class));

        Dataset<IntUnpivotObservationSchema> intUnpivotObservation =
                upstream.get(IntUnpivotObservationModel.class)
                        .as(Encoders.bean(IntUnpivotObservationSchema.class));

        return IntStationForecastObservationProcessor.process(
                        intUnpivotObservation, stgMeteoFrancePaquet, intStationGridMapping)
                .toDF();
    }

    // --- Static factory method ---
    public static IntStationForecastObservationModel.Builder builder() {
        return new IntStationForecastObservationModel.Builder();
    }

    public static class Builder extends SparkModel.Builder<IntStationForecastObservationModel> {
        @Override
        public IntStationForecastObservationModel build() {
            return new IntStationForecastObservationModel(this);
        }
    }

    @Override
    public ModelWriteResult writeToTable(Dataset<Row> dataset, PipelineContext context) {
        return CloudConfidenceIcebergTableWriter.merge(dataset, location, primaryKey);
    }
}
