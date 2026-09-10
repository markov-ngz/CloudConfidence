package ngz.cloudconfidence.sparkmodels.models.seed;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import ngz.markov.sparkmodel.execution.DependencyResolver;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.model.Layer;
import ngz.markov.sparkmodel.model.Materialization;
import ngz.markov.sparkmodel.model.SparkModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public class SeedStandardizedVariable extends SparkModel {

    private SeedStandardizedVariable(SeedStandardizedVariable.Builder builder) {

        super(builder);
    }

    public static SeedStandardizedVariable.Builder builder() {
        return new SeedStandardizedVariable.Builder();
    }

    public static class Builder extends SparkModel.Builder<SeedStandardizedVariable> {
        @Override
        public SeedStandardizedVariable build() {
            return new SeedStandardizedVariable(this);
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
        return SeedStandardizedVariable.Schema.class; // direct, no wrapping
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {
        return read(pipelineContext);
    }

    @Data
    public static class Schema implements Serializable {

        private String variableName;
        private String standardizedVariableName;
    }
}
