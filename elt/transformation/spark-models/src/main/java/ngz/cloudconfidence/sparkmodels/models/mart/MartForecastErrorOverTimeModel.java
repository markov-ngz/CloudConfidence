package ngz.cloudconfidence.sparkmodels.models.mart;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.application.CloudConfidenceIcebergTableWriter;
import ngz.cloudconfidence.sparkmodels.models.intermediate.IntAggForecastErrorOverTimeModel;
import ngz.cloudconfidence.sparkmodels.models.intermediate.IntAggForecastErrorOverTimeSchema;
import ngz.markov.sparkmodel.execution.DependencyResolver;
import ngz.markov.sparkmodel.execution.ModelWriteResult;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.model.Layer;
import ngz.markov.sparkmodel.model.Materialization;
import ngz.markov.sparkmodel.model.SparkModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

public class MartForecastErrorOverTimeModel extends SparkModel {

    public static final List<Class<? extends SparkModel>> DEPENDENCIES =
            List.of(IntAggForecastErrorOverTimeModel.class);

    private MartForecastErrorOverTimeModel(MartForecastErrorOverTimeModel.Builder builder) {

        super(builder);
    }

    @Override
    protected Layer defaultNodeType() {
        return Layer.MART;
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
        return MartForecastErrorOverTime.class;
    }

    // --- Static factory method ---
    public static MartForecastErrorOverTimeModel.Builder builder() {
        return new MartForecastErrorOverTimeModel.Builder();
    }

    public static class Builder extends SparkModel.Builder<MartForecastErrorOverTimeModel> {
        @Override
        public MartForecastErrorOverTimeModel build() {
            return new MartForecastErrorOverTimeModel(this);
        }
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {
        Dataset<IntAggForecastErrorOverTimeSchema> intAggForecastErrorOverTime =
                upstream.get(IntAggForecastErrorOverTimeModel.class)
                        .as(Encoders.bean(IntAggForecastErrorOverTimeSchema.class));

        return MartForecastErrorOverTimeProcessor.process(intAggForecastErrorOverTime).toDF();
    }

    @Override
    protected ModelWriteResult writeToTable(Dataset<Row> dataset, PipelineContext context) {
        return CloudConfidenceIcebergTableWriter.createOrReplaceTable(
                dataset, location, new MartForecastErrorOverTimeAudit());
    }
}
