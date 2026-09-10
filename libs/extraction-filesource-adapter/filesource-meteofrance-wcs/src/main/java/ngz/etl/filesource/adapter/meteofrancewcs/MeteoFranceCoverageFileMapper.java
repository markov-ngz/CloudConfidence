package ngz.etl.filesource.adapter.meteofrancewcs;

import java.util.HashMap;
import java.util.Map;
import ngz.etl.core.file.FileInformation;
import ngz.meteofrance.wcs.model.MeteoFranceCoverageFile;

class MeteoFranceCoverageFileMapper {

    public static FileInformation mapToFileInformation(
            MeteoFranceCoverageFile meteoFranceCoverageFile) {

        Map<String, String> metadata = new HashMap<>();

        metadata.put("model", meteoFranceCoverageFile.getModel());
        metadata.put("modelType", meteoFranceCoverageFile.getModelType());
        metadata.put(
                "referenceTime", meteoFranceCoverageFile.getCoverageReferenceTime().toString());
        meteoFranceCoverageFile
                .getDimensions()
                .forEach(
                        (k, v) -> {
                            metadata.put(k, v);
                        });

        return FileInformation.builder()
                .id(buildFileInformationId(meteoFranceCoverageFile))
                .location(meteoFranceCoverageFile.getUrl())
                .format(meteoFranceCoverageFile.getFormat())
                .metadata(metadata)
                .build();
    }

    private static String buildFileInformationId(MeteoFranceCoverageFile meteoFranceCoverageFile) {

        StringBuilder idBuilder = new StringBuilder();
        idBuilder.append(meteoFranceCoverageFile.getCoverageId());
        idBuilder.append("__");

        // Add dimensions in sorted order for consistency
        meteoFranceCoverageFile.getDimensions().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        entry -> {

                            // not first element so add a _ to delimit
                            if (idBuilder.length() > 0 && !idBuilder.toString().endsWith("__")) {
                                idBuilder.append("_");
                            }

                            // add dim=value
                            idBuilder.append(entry.getKey()).append("=").append(entry.getValue());
                        });

        return idBuilder.toString();
    }
}
