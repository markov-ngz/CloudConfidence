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

public class SeedStandardizedUnit extends SparkModel {
    private SeedStandardizedUnit(SeedStandardizedUnit.Builder builder) {

        super(builder);
    }

    public static SeedStandardizedUnit.Builder builder() {
        return new SeedStandardizedUnit.Builder();
    }

    public static class Builder extends SparkModel.Builder<SeedStandardizedUnit> {
        @Override
        public SeedStandardizedUnit build() {
            return new SeedStandardizedUnit(this);
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
        return SeedStandardizedUnit.Schema.class; // direct, no wrapping
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {
        return read(pipelineContext);
    }

    @Data
    public static class Schema implements Serializable {

        String unitShortName;
        String coefficient;
        String offset;
        String standardizedUnit;
    }
}
