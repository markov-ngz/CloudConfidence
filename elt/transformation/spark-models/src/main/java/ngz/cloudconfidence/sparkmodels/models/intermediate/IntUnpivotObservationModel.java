package ngz.cloudconfidence.sparkmodels.models.intermediate;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedMeteoFranceObservationVariable;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedUnit;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedVariable;
import ngz.cloudconfidence.sparkmodels.models.staging.StgMeteoFranceObservationModel;
import ngz.cloudconfidence.sparkmodels.models.staging.StgMeteoFranceObservationSchema;
import ngz.markov.sparkmodel.execution.DependencyResolver;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.model.Layer;
import ngz.markov.sparkmodel.model.Materialization;
import ngz.markov.sparkmodel.model.SparkModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

public class IntUnpivotObservationModel extends SparkModel {

    public static final List<Class<? extends SparkModel>> DEPENDENCIES =
            List.of(
                    StgMeteoFranceObservationModel.class,
                    SeedMeteoFranceObservationVariable.class,
                    SeedStandardizedVariable.class,
                    SeedStandardizedUnit.class);

    private IntUnpivotObservationModel(IntUnpivotObservationModel.Builder builder) {

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
        return IntUnpivotObservationSchema.class;
    }

    // --- Static factory method ---
    public static IntUnpivotObservationModel.Builder builder() {
        return new IntUnpivotObservationModel.Builder();
    }

    public static class Builder extends SparkModel.Builder<IntUnpivotObservationModel> {
        @Override
        public IntUnpivotObservationModel build() {
            return new IntUnpivotObservationModel(this);
        }
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {
        Dataset<StgMeteoFranceObservationSchema> stgMeteoFranceObservation =
                upstream.get(StgMeteoFranceObservationModel.class)
                        .as(Encoders.bean(StgMeteoFranceObservationSchema.class));

        Dataset<SeedMeteoFranceObservationVariable.Schema> seedMeteofranceObservationVariable =
                upstream.get(SeedMeteoFranceObservationVariable.class)
                        .as(Encoders.bean(SeedMeteoFranceObservationVariable.Schema.class));

        Dataset<SeedStandardizedVariable.Schema> seedVariables =
                upstream.get(SeedStandardizedVariable.class)
                        .as(Encoders.bean(SeedStandardizedVariable.Schema.class));

        Dataset<SeedStandardizedUnit.Schema> seedUnits =
                upstream.get(SeedStandardizedUnit.class)
                        .as(Encoders.bean(SeedStandardizedUnit.Schema.class));

        Dataset<IntUnpivotObservationSchema> result =
                IntUnpivotObservationProcessor.process(
                        pipelineContext.lookBackWindow(),
                        stgMeteoFranceObservation,
                        seedMeteofranceObservationVariable,
                        seedVariables,
                        seedUnits);

        return result.toDF();
    }
}
