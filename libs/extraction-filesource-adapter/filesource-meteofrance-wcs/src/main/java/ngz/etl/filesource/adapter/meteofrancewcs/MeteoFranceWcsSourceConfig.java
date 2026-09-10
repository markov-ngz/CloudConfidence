package ngz.etl.filesource.adapter.meteofrancewcs;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ngz.meteofrance.wcs.service.MeteoFranceWcsService;

public class MeteoFranceWcsSourceConfig {

    private final Instant startTime;
    private final Instant endTime;
    private final String model;
    private final MeteoFranceWcsService.ModelType modelType;
    private final List<String> requestedCoverageTitles;

    public static MeteoFranceWcsSourceConfig load(Map<String, String> props) {
        return new MeteoFranceWcsSourceConfig(props);
    }

    public MeteoFranceWcsSourceConfig(Map<String, String> props) {
        // 1. Parse Instants (handling potential nulls or empty strings gracefully)
        this.startTime =
                Optional.ofNullable(props.get("starttime"))
                        .map(Instant::parse)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Missing required property: starttime"));

        this.endTime =
                Optional.ofNullable(props.get("endtime"))
                        .map(Instant::parse)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Missing required property: endtime"));

        // 2. Map simple String
        this.model = props.get("model");

        //
        this.modelType = MeteoFranceWcsService.ModelType.valueOf(props.get("modeltype"));

        // 3. Split the pipe-separated string into a List
        String coverageTitlesStr = props.get("coveragetitles");
        if (coverageTitlesStr != null && !coverageTitlesStr.isBlank()) {
            this.requestedCoverageTitles = Arrays.asList(coverageTitlesStr.split("\\|"));
        } else {
            this.requestedCoverageTitles = Collections.emptyList();
        }
    }

    // --- Getters ---

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public String getModel() {
        return model;
    }

    public MeteoFranceWcsService.ModelType getModelType() {
        return modelType;
    }

    /** Returns an unmodifiable view of the titles to preserve immutability. */
    public List<String> getRequestedCoverageTitles() {
        return Collections.unmodifiableList(requestedCoverageTitles);
    }
}
