package ngz.cloudconfidence.sparkmodels.infrastructure.iceberg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public class WriteRequest {

    @Getter final Dataset<Row> sourceDataset;
    @Getter final String targetTable;
    @Getter final List<String> primaryKeys;
    @Getter final WriteStrategy strategy;
    @Getter final Map<String, String> options; // e.g. "branch" -> "my-branch"

    private WriteRequest(Builder builder) {
        this.sourceDataset = builder.sourceDataset;
        this.targetTable = builder.targetTable;
        this.primaryKeys = List.copyOf(builder.primaryKeys);
        this.strategy = builder.strategy;
        this.options = Map.copyOf(builder.options);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Dataset<Row> sourceDataset;
        private String targetTable;
        private List<String> primaryKeys = List.of();
        private WriteStrategy strategy;
        private Map<String, String> options = new HashMap<>();

        public Builder sourceDataset(Dataset<Row> ds) {
            this.sourceDataset = ds;
            return this;
        }

        public Builder targetTable(String table) {
            this.targetTable = table;
            return this;
        }

        public Builder primaryKeys(List<String> keys) {
            this.primaryKeys = keys;
            return this;
        }

        public Builder strategy(WriteStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder option(String key, String value) {
            this.options.put(key, value);
            return this;
        }

        public Builder branch(String branchName) {
            return option("branch", branchName);
        }

        public WriteRequest build() {
            Objects.requireNonNull(sourceDataset, "sourceDataset is required");
            Objects.requireNonNull(targetTable, "targetTable is required");
            Objects.requireNonNull(strategy, "strategy is required");
            return new WriteRequest(this);
        }
    }
}
