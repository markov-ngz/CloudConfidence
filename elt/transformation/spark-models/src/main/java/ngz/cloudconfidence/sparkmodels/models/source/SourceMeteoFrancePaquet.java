package ngz.cloudconfidence.sparkmodels.models.source;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import ngz.markov.sparkmodel.execution.DependencyResolver;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.model.Layer;
import ngz.markov.sparkmodel.model.Materialization;
import ngz.markov.sparkmodel.model.SparkColumn;
import ngz.markov.sparkmodel.model.SparkModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public class SourceMeteoFrancePaquet extends SparkModel {

    private SourceMeteoFrancePaquet(SourceMeteoFrancePaquet.Builder builder) {

        super(builder);
    }

    public static SourceMeteoFrancePaquet.Builder builder() {
        return new SourceMeteoFrancePaquet.Builder();
    }

    public static class Builder extends SparkModel.Builder<SourceMeteoFrancePaquet> {
        @Override
        public SourceMeteoFrancePaquet build() {
            return new SourceMeteoFrancePaquet(this);
        }
    }

    @Override
    protected Layer defaultNodeType() {
        return Layer.SOURCE;
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
        return Schema.class; // direct, no wrapping
    }

    @Override
    public Dataset<Row> process(PipelineContext pipelineContext, DependencyResolver upstream) {
        Dataset<Row> ds = read(pipelineContext);
        ds.show();
        return ds;
    }

    @Data
    public static class Schema implements Serializable {

        @SparkColumn(name = "meteofrance_previnum")
        private String meteofrancePrevinum;

        @SparkColumn(name = "meteofrance_model")
        private String meteofranceModel;

        @SparkColumn(name = "meteofrance_grid")
        private String meteofranceGrid;

        @SparkColumn(name = "meteofrance_package")
        private String meteofrancePackage;

        @SparkColumn(name = "meteofrance_reference_time")
        private String meteofranceReferenceTime;

        @SparkColumn(name = "meteofrance_time")
        private String meteofranceTime;

        private String validTime;

        private String startTime;

        private String endTime;

        private String levelType;

        private Double levelValue;

        private Boolean isLatLon;

        private String projection;

        private Integer gridX;

        private Integer gridY;

        private Double lat;

        private Double lon;

        private String variable;

        private String unit;

        private Double value;
    }
}
