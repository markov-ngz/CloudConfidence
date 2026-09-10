package ngz.cloudconfidence.convertgrib2.configuration;

import java.util.Arrays;
import java.util.Map;
import lombok.Getter;
import ngz.extraction.core.sink.SunkFile;
import tools.jackson.databind.ObjectMapper;

@Getter
public class Configuration {

    private final Map<String, String> props;

    private final String jobId;
    private final String jobRunId;

    private final String outputDir;
    private final org.apache.hadoop.conf.Configuration hadoopConfig;

    private SunkFile[] sunkFiles;
    private static ObjectMapper objectMapper = new ObjectMapper();

    private String xcomPath = "/airflow/xcom/return.json";

    public static Configuration load(Map<String, String> props) {
        return new Configuration(props);
    }

    public Configuration(Map<String, String> props) {
        this.props = props;

        // Execution metadata
        this.jobId = getRequiredValue("etl.jobId");
        this.jobRunId = getRequiredValue("etl.jobRunId");
        this.sunkFiles =
                Arrays.stream(
                                objectMapper.readValue(
                                        getRequiredValue("etl.sunkfiles"), SunkFile[].class))
                        .map(
                                file -> {
                                    file.setUri(replaceS3prefix(file.getUri()));
                                    return file;
                                })
                        .toArray(SunkFile[]::new);

        // Target output
        this.outputDir = replaceS3prefix(getRequiredValue("etl.outputdir"));

        // Hadoop config
        Map<String, String> hadoopProperties = ConfigurationUtils.subMap(props, "hadoop");
        this.hadoopConfig = new org.apache.hadoop.conf.Configuration();
        hadoopProperties.forEach(this.hadoopConfig::set);

        // Airflow specific
        if (props.containsKey("etl.airflow.xcompath")) {
            this.xcomPath = props.get("etl.airflow.xcompath");
        }
    }

    private String getRequiredValue(String key) {
        String value = props.get(key.toLowerCase());
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Required property '" + key + "' is missing or empty.");
        }
        return value;
    }

    private static String replaceS3prefix(String uri) {
        if (uri.startsWith("s3://")) {
            return uri.replace("s3://", "s3a://");
        }
        return uri;
    }
}
