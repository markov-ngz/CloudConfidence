package ngz.cloudconfidence.sparkmodels.models.staging;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.application.CloudConfidenceIcebergTableWriter;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedUnit;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedVariable;
import ngz.cloudconfidence.sparkmodels.models.source.SourceMeteoFrancePaquet;
import ngz.markov.sparkmodel.execution.DependencyResolver;
import ngz.markov.sparkmodel.execution.ModelWriteResult;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.model.Layer;
import ngz.markov.sparkmodel.model.Materialization;
import ngz.markov.sparkmodel.model.SparkModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

public class StgMeteoFrancePaquetModel extends SparkModel {

    public static final List<Class<? extends SparkModel>> DEPENDENCIES =
            List.of(
                    SourceMeteoFrancePaquet.class,
                    SeedStandardizedVariable.class,
                    SeedStandardizedUnit.class);

    private StgMeteoFrancePaquetModel(Builder builder) {

        super(builder);
    }

    @Override
    protected Layer defaultNodeType() {
        return Layer.STAGING;
    }

    @Override
    protected List<Class<? extends SparkModel>> defaultDeps() {
        return DEPENDENCIES;
    }

    @Override
    protected List<String> defaultPrimaryKey() {
        return List.of(
                "meteofranceModel",
                "meteofranceGrid",
                "meteofrancePackage",
                "meteofranceReferenceTime",
                "meteofranceTime",
                "latitude",
                "longitude",
                "variable");
    }

    @Override
    protected Materialization defaultMaterialization() {
        return Materialization.EPHEMERAL;
    }

    @Override
    public Class<?> schemaClass() {
        return StgMeteoFrancePaquetSchema.class;
    }

    // --- Static factory method ---
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends SparkModel.Builder<StgMeteoFrancePaquetModel> {
        @Override
        public StgMeteoFrancePaquetModel build() {
            return new StgMeteoFrancePaquetModel(this);
        }
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {
        Dataset<SourceMeteoFrancePaquet.Schema> source =
                upstream.get(SourceMeteoFrancePaquet.class)
                        .as(Encoders.bean(SourceMeteoFrancePaquet.Schema.class));

        Dataset<SeedStandardizedVariable.Schema> seedVariables =
                upstream.get(SeedStandardizedVariable.class)
                        .as(Encoders.bean(SeedStandardizedVariable.Schema.class));

        Dataset<SeedStandardizedUnit.Schema> seedUnits =
                upstream.get(SeedStandardizedUnit.class)
                        .as(Encoders.bean(SeedStandardizedUnit.Schema.class));

        Dataset<StgMeteoFrancePaquetSchema> result =
                StgMeteoFrancePaquetProcessor.process(
                        source, seedVariables, seedUnits, pipelineContext.lookBackWindow());

        return result.toDF();
    }

    @Override
    public ModelWriteResult writeToTable(Dataset<Row> dataset, PipelineContext context) {
        return CloudConfidenceIcebergTableWriter.merge(dataset, location, primaryKey);
    }
}
