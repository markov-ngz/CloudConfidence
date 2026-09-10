package ngz.etl.filesource.adapter.meteofrance.paquet;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MeteoFrancePaquetFileSourceConfig {

    // Hardcoded configuration set
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
    private static final String DATE_ZONE = "Europe/Paris";

    private final String applicationId;
    private final String previnum;
    private final String model;
    private final String grid;
    private final List<String> packageNames;
    private final Instant startTime;
    private final Instant endTime;

    private MeteoFrancePaquetFileSourceConfig(
            String applicationId,
            String previnum,
            String model,
            String grid,
            List<String> packageNames,
            Instant startTime,
            Instant endTime) {
        this.applicationId = applicationId;
        this.previnum = previnum;
        this.model = model;
        this.grid = grid;
        this.packageNames = packageNames;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static MeteoFrancePaquetFileSourceConfig fromMap(Map<String, String> props) {
        String applicationId = getRequiredValueStatic(props, "applicationid");

        String previnum = getRequiredValueStatic(props, "previnum");
        String model = getRequiredValueStatic(props, "model");
        String grid = getRequiredValueStatic(props, "grid");

        // Parse the package names
        String packagesRaw = getRequiredValueStatic(props, "packagenames");
        List<String> packageNames =
                Arrays.stream(packagesRaw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

        // Create the formatter using your custom pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);

        // Parse the hardcoded config strings using the formatter and convert to Instant (UTC)
        Instant startTime =
                LocalDateTime.parse(getRequiredValueStatic(props, "starttime"), formatter)
                        .atZone(ZoneId.of(DATE_ZONE))
                        .toInstant();
        Instant endTime =
                LocalDateTime.parse(getRequiredValueStatic(props, "endtime"), formatter)
                        .atZone(ZoneId.of(DATE_ZONE))
                        .toInstant();

        return new MeteoFrancePaquetFileSourceConfig(
                applicationId, previnum, model, grid, packageNames, startTime, endTime);
    }

    private static String getRequiredValueStatic(Map<String, String> props, String key) {
        String value = props.get(key.toLowerCase());
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Required property '" + key + "' is missing or empty.");
        }
        return value;
    }

    // --- Getters ---
    public String getApplicationId() {
        return applicationId;
    }

    public String getPrevinum() {
        return previnum;
    }

    public String getModel() {
        return model;
    }

    public String getGrid() {
        return grid;
    }

    public List<String> getPackageNames() {
        return Collections.unmodifiableList(packageNames);
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }
}
