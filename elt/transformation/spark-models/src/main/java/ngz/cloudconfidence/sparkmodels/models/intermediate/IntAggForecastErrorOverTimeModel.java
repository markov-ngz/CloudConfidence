package ngz.cloudconfidence.sparkmodels.models.intermediate;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.application.CloudConfidenceIcebergTableWriter;
import ngz.markov.sparkmodel.execution.DependencyResolver;
import ngz.markov.sparkmodel.execution.ModelWriteResult;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.model.Layer;
import ngz.markov.sparkmodel.model.Materialization;
import ngz.markov.sparkmodel.model.SparkModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

public class IntAggForecastErrorOverTimeModel extends SparkModel {

    public static final List<Class<? extends SparkModel>> DEPENDENCIES =
            List.of(IntStationForecastObservationModel.class);

    private IntAggForecastErrorOverTimeModel(IntAggForecastErrorOverTimeModel.Builder builder) {

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

        return List.of("stationId", "forecastModel", "time", "variable");
    }

    @Override
    protected Materialization defaultMaterialization() {
        return Materialization.EPHEMERAL;
    }

    @Override
    public Class<?> schemaClass() {
        return IntAggForecastErrorOverTimeSchema.class;
    }

    // --- Static factory method ---
    public static IntAggForecastErrorOverTimeModel.Builder builder() {
        return new IntAggForecastErrorOverTimeModel.Builder();
    }

    public static class Builder extends SparkModel.Builder<IntAggForecastErrorOverTimeModel> {
        @Override
        public IntAggForecastErrorOverTimeModel build() {
            return new IntAggForecastErrorOverTimeModel(this);
        }
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {

        Dataset<IntStationForecastObservationSchema> intStationForecastObservation =
                upstream.get(IntStationForecastObservationModel.class)
                        .as(Encoders.bean(IntStationForecastObservationSchema.class));

        return IntAggForecastErrorOverTimeProcessor.process(
                        intStationForecastObservation, pipelineContext.lookBackWindow())
                .toDF();
    }

    @Override
    public ModelWriteResult writeToTable(Dataset<Row> dataset, PipelineContext context) {
        return CloudConfidenceIcebergTableWriter.merge(dataset, location, primaryKey);
    }
}
