package ngz.cloudconfidence.sparkmodels.configuration;

import java.util.Map;
import lombok.Getter;

@Getter
public class CloudConfidenceConfiguration {

    public static final String ENV_PREFIX = "CC";

    private final String modelRegistryPath;
    private final String pipelineContextPath;
    private final Map<String, String> pipelineProps;
    private final String selector;
    private final String selectorDelimiter = ":";
    private final String xcomPath;

    private final String polarisCredentials;

    public static CloudConfidenceConfiguration load(String[] args) {
        Map<String, String> props =
                ConfigurationLoader.loadProperties(args, CloudConfidenceConfiguration.ENV_PREFIX);
        return new CloudConfidenceConfiguration(props);
    }

    public CloudConfidenceConfiguration(Map<String, String> props) {

        this.polarisCredentials = getRequiredValue(props, "polaris.credential");

        this.selector = getRequiredValue(props, "select");

        this.modelRegistryPath =
                props.getOrDefault("modelregistry.path".toLowerCase(), "./model_registry");

        this.pipelineContextPath = props.get("pipelinecontext.path".toLowerCase());

        this.pipelineProps = ConfigurationLoader.subMap(props, "pipelinecontext");

        this.xcomPath = props.getOrDefault("airflow.xcompath", "/airflow/xcom/return.json");
    }

    private String getRequiredValue(Map<String, String> props, String key) {
        String value = props.get(key.toLowerCase());
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Required property '" + key + "' is missing or empty.");
        }
        return value;
    }
}
