package ngz.cloudconfidence.elt.extraction.config;

import java.util.Map;

/** Main Configuration loading. */
public class Configuration {

    private String jobId;

    private String sourcePropertiesPrefix = "etl.source";
    private String sinkPropertiesPrefix = "etl.sink";

    private Map<String, String> sourceProperties;
    private Map<String, String> sinkProperties;

    // 2 ways to instantiate the object
    public static Configuration load(Map<String, String> props) {
        return new Configuration(props);
    }

    public Configuration(Map<String, String> props) {

        if (props.isEmpty()) {
            throw new IllegalArgumentException("Argument props cannot be an empty map");
        }

        System.out.println(props);

        this.sourceProperties = ConfigurationUtils.subMap(props, this.sourcePropertiesPrefix);
        this.sinkProperties = ConfigurationUtils.subMap(props, this.sinkPropertiesPrefix);

        if (!props.containsKey("etl.jobid")) {
            throw new IllegalArgumentException("Property 'etl.jobid' not specified");
        }

        this.jobId = props.get("etl.jobid");
    }

    public String getJobId() {
        return this.jobId;
    }

    public Map<String, String> getSourceProperties() {
        return this.sourceProperties;
    }

    public Map<String, String> getSinkProperties() {
        return this.sinkProperties;
    }
}
