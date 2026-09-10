package ngz.etl.filesource.adapter.meteofrance.observation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import ngz.meteofrance.observation.api.Format;

public class MeteoFranceObsFileSourceConfig {

    private Format format = Format.csv;
    private final List<String> departmentIds;
    private final String applicationId; // meteofrance client

    private MeteoFranceObsFileSourceConfig(String applicationId, List<String> departmentIds) {
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId cannot be null");
        this.departmentIds = departmentIds;
    }

    private MeteoFranceObsFileSourceConfig(
            String applicationId, List<String> departmentIds, Format format) {
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId cannot be null");
        this.departmentIds = departmentIds;
        this.format = format;
    }

    public Format getFormat() {
        return format;
    }

    public List<String> getDepartmentIds() {
        return departmentIds;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public static MeteoFranceObsFileSourceConfig fromMap(Map<String, String> props) {

        String applicationId = props.get("applicationid");
        List<String> departmentIds = List.of((props.get("departmentids").split("\\|")));

        if (props.containsKey("format")) {
            Format format = Format.valueOf(props.get("format"));
            return new MeteoFranceObsFileSourceConfig(applicationId, departmentIds, format);
        }

        return new MeteoFranceObsFileSourceConfig(applicationId, departmentIds);
    }
}
