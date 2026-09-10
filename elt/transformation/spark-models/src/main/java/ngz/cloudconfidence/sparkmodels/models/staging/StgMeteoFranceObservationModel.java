package ngz.cloudconfidence.sparkmodels.models.staging;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.application.CloudConfidenceIcebergTableWriter;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedMeteoFranceObservationVariable;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedMeteoFranceStation;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedVariable;
import ngz.cloudconfidence.sparkmodels.models.source.SourceMeteoFranceObservation;
import ngz.markov.sparkmodel.execution.DependencyResolver;
import ngz.markov.sparkmodel.execution.ModelWriteResult;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.model.Layer;
import ngz.markov.sparkmodel.model.Materialization;
import ngz.markov.sparkmodel.model.SparkModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

public class StgMeteoFranceObservationModel extends SparkModel {

    public static final List<Class<? extends SparkModel>> DEPENDENCIES =
            List.of(
                    SourceMeteoFranceObservation.class,
                    SeedMeteoFranceObservationVariable.class,
                    SeedMeteoFranceStation.class,
                    SeedStandardizedVariable.class,
                    SeedStandardizedVariable.class);

    @Override
    protected List<Class<? extends SparkModel>> defaultDeps() {
        return DEPENDENCIES;
    }

    @Override
    protected List<String> defaultPrimaryKey() {
        return List.of("stationId", "observationTime");
    }

    @Override
    protected Materialization defaultMaterialization() {
        return null;
    }

    @Override
    protected Layer defaultNodeType() {
        return Layer.STAGING;
    }

    @Override
    public Class<?> schemaClass() {
        return StgMeteoFranceObservationSchema.class;
    }

    private StgMeteoFranceObservationModel(StgMeteoFranceObservationModel.Builder builder) {

        super(builder);
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {
        Dataset<SourceMeteoFranceObservation.Schema> source =
                upstream.get(SourceMeteoFranceObservation.class)
                        .as(Encoders.bean(SourceMeteoFranceObservation.Schema.class));

        Dataset<SeedMeteoFranceObservationVariable.Schema> meteofranceObservationVariable =
                upstream.get(SeedMeteoFranceObservationVariable.class)
                        .as(Encoders.bean(SeedMeteoFranceObservationVariable.Schema.class));

        Dataset<SeedMeteoFranceStation.Schema> meteofranceStations =
                upstream.get(SeedMeteoFranceStation.class)
                        .as(Encoders.bean(SeedMeteoFranceStation.Schema.class));

        Dataset<SeedStandardizedVariable.Schema> seedVariables =
                upstream.get(SeedStandardizedVariable.class)
                        .as(Encoders.bean(SeedStandardizedVariable.Schema.class));

        // 2. Execute business processor with the dynamic lookBackWindow
        Dataset<StgMeteoFranceObservationSchema> result =
                StgMeteoFranceObservationProcessor.process(
                        source,
                        meteofranceObservationVariable,
                        meteofranceStations,
                        seedVariables,
                        pipelineContext.lookBackWindow());

        return result.toDF();
    }

    // --- Static factory method ---
    public static StgMeteoFranceObservationModel.Builder builder() {
        return new StgMeteoFranceObservationModel.Builder();
    }

    public static class Builder extends SparkModel.Builder<StgMeteoFranceObservationModel> {
        @Override
        public StgMeteoFranceObservationModel build() {
            return new StgMeteoFranceObservationModel(this);
        }
    }

    // Write behaviour

    @Override
    public ModelWriteResult writeToTable(Dataset<Row> dataset, PipelineContext context) {
        return CloudConfidenceIcebergTableWriter.merge(dataset, location, primaryKey);
    }
}
