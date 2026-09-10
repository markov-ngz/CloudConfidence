package ngz.etl.filesource.adapter.meteofrance.observation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ngz.extraction.core.model.FileInformation;
import ngz.extraction.core.source.FileSource;
import ngz.extraction.core.source.FileSourceProvider;
import ngz.meteofrance.observation.api.Format;
import ngz.meteofrance.observation.model.ProductData;
import ngz.meteofrance.observation.service.StationService;

@FileSourceProvider(MeteoFranceObsFileSourceFactory.class)
public class MeteoFranceObsFileSource implements FileSource {
    private final transient MeteoFranceObsFileSourceFactory factory =
            new MeteoFranceObsFileSourceFactory();

    private final StationService stationService;

    private final List<String> departmentIds;
    private final Format format;

    public MeteoFranceObsFileSource(
            StationService stationService, List<String> departmentIds, Format format) {
        this.stationService = stationService;
        this.departmentIds = departmentIds;
        this.format = format;
    }

    @Override
    public List<FileInformation> listFiles() {

        List<FileInformation> fileInformations = new ArrayList<>();

        for (String departmentId : this.departmentIds) {
            FileInformation fileInformation =
                    FileInformation.builder()
                            .id("package-obs-dpt-" + departmentId)
                            .location(departmentId) // location is how this class can download it
                            .format(this.format.name())
                            .metadata(Map.of())
                            .build();
            fileInformations.add(fileInformation);
        }

        return fileInformations;
    }

    @Override
    public byte[] downloadFile(String fileLocation) {
        ProductData productData =
                this.stationService.downloadObservationsByDepartmentId(fileLocation, this.format);
        return productData.getContent();
    }
}
