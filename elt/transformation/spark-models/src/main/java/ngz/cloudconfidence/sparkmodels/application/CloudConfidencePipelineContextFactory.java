package ngz.cloudconfidence.sparkmodels.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.CustomIcebergMetricsReporter;
import ngz.cloudconfidence.sparkmodels.models.functions.UdfHaversineKm;
import ngz.markov.sparkmodel.execution.context.InvalidPipelineContextException;
import ngz.markov.sparkmodel.execution.context.LookBackWindow;
import ngz.markov.sparkmodel.execution.context.PipelineContext;
import ngz.markov.sparkmodel.execution.context.PipelineContextFactory;
import org.apache.spark.sql.SparkSession;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

public class CloudConfidencePipelineContextFactory implements PipelineContextFactory {

    private static final String LOOKBACK_WINDOW_START_KEY = "lookbackwindow.starttime";
    private static final String LOOKBACK_WINDOW_END_KEY = "lookbackwindow.endtime";
    private static final String PROPERTIES_KEY = "property";
    private static final String DRY_RUN_KEY = "dryrun";
    private final String pipelineContextPath;
    private final Map<String, String> contextProps;
    private final String polarisCredential;

    public CloudConfidencePipelineContextFactory(
            String pipelineContextPath,
            Map<String, String> contextProps,
            String polarisCredential) {
        this.pipelineContextPath = pipelineContextPath;
        this.contextProps = contextProps;
        this.polarisCredential = polarisCredential;
    }

    @Override
    public PipelineContext create(SparkSession spark) {

        // 1. Read the file (YAML/JSON) into PipelineContextSpec
        PipelineContextSpec spec;
        if (pipelineContextPath != null) {
            spec = readPipelineContextFile(pipelineContextPath);
        } else {
            spec = new PipelineContextSpec();
        }

        // 2. Merge properties: file properties are overridden by contextProps
        Map<String, String> mergedProperties =
                mergeProperties(spec, getCommaSeparatedKv(contextProps.get(PROPERTIES_KEY)));

        // 3. Lookback window (override with contextProps if specified)
        String lookbackWindowStartStr =
                contextProps.getOrDefault(
                        LOOKBACK_WINDOW_START_KEY,
                        spec.getLookbackWindow() != null
                                ? spec.getLookbackWindow().getStart().toString()
                                : "1970-01-01T00:00:00Z");
        String lookbackWindowEndStr =
                contextProps.getOrDefault(
                        LOOKBACK_WINDOW_END_KEY,
                        spec.getLookbackWindow() != null
                                ? spec.getLookbackWindow().getEnd().toString()
                                : "2099-01-01T00:00:00Z");

        LookBackWindow lookBackWindow =
                new LookBackWindow(
                        Timestamp.from(Instant.parse(lookbackWindowStartStr)),
                        Timestamp.from(Instant.parse(lookbackWindowEndStr)));

        // 4. Dry run (override with contextProps if specified)
        boolean isDryRun =
                Boolean.parseBoolean(
                        contextProps.getOrDefault(DRY_RUN_KEY, String.valueOf(spec.isDryRun())));

        // 5. Configure Spark session
        configureSparkSession(spark);

        // 6. Instantiate and return PipelineContext
        return new PipelineContext(spark, lookBackWindow, mergedProperties, isDryRun);
    }

    private static PipelineContextSpec readPipelineContextFile(String filePath) {

        try {

            ObjectMapper mapper;
            if (filePath.toLowerCase().endsWith(".json")) {
                mapper = new ObjectMapper();
            } else if (filePath.toLowerCase().endsWith(".yaml")
                    || filePath.toLowerCase().endsWith(".yml")) {
                mapper = new ObjectMapper(new YAMLFactory());
            } else {
                throw new IllegalArgumentException(
                        "Unsupported file extension. Only .json, .yaml, or .yml are supported: "
                                + filePath);
            }
            TopPipelineContextSpec topPipelineContextSpec =
                    mapper.readValue(new File(filePath), TopPipelineContextSpec.class);
            return topPipelineContextSpec.pipelineContextSpec;
        } catch (JacksonException e) {
            throw new InvalidPipelineContextException("Failed to read pipeline from disk", e);
        }
    }

    private static Map<String, String> mergeProperties(
            PipelineContextSpec spec, Map<String, String> contextProps) {
        Map<String, String> merged = new HashMap<>();
        // Add properties from the file (if any)
        if (spec.getCustomConfig() != null) {
            merged.putAll(spec.getCustomConfig());
        }
        // Override with contextProps
        merged.putAll(contextProps);
        return merged;
    }

    @Data
    public static class TopPipelineContextSpec {
        @JsonProperty(value = "pipeline_context")
        private PipelineContextSpec pipelineContextSpec;
    }

    @Data
    public static class PipelineContextSpec {
        @JsonProperty(value = "lookback_window", required = false)
        private LookBackWindowSpec lookbackWindow;

        @JsonProperty(value = "custom_config", required = false)
        private Map<String, String> customConfig;

        @JsonProperty(value = "dry_run", required = false)
        private boolean dryRun = false; // Default to false

        @Data
        public static class LookBackWindowSpec {
            @JsonProperty("start_time")
            private String start;

            @JsonProperty("end_time")
            private String end;

            public Timestamp getStart() {
                return start != null ? Timestamp.from(Instant.parse(start)) : null;
            }

            public Timestamp getEnd() {
                return end != null ? Timestamp.from(Instant.parse(end)) : null;
            }
        }
    }

    private static Map<String, String> getCommaSeparatedKv(String props) {

        if (Objects.isNull(props)) {
            return Map.of();
        }

        return Arrays.stream(props.split(","))
                .filter(kv -> !kv.isEmpty()) // Filter out empty strings
                .map(kv -> kv.split("="))
                .filter(kv -> kv.length == 2) // Ensure each entry has a key and value
                .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
    }

    public void configureSparkSession(SparkSession spark) {
        spark.conf().set("spark.sql.catalog.rest.credential", polarisCredential);
        spark.conf()
                .set(
                        "spark.sql.catalog.rest.metrics-reporter-impl",
                        CustomIcebergMetricsReporter.class.getName());
        UdfHaversineKm.register(spark);
    }
}
