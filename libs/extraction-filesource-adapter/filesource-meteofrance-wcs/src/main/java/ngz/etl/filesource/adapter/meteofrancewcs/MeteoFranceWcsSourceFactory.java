package ngz.etl.filesource.adapter.meteofrancewcs;

import java.util.Map;
import ngz.etl.core.file.FileSource;
import ngz.etl.core.file.FileSourceFactory;
import ngz.meteofrance.core.client.MeteoFranceClient;
import ngz.meteofrance.core.client.MeteoFranceClientFactory;
import ngz.meteofrance.wcs.service.MeteoFranceWcsService;

public class MeteoFranceWcsSourceFactory implements FileSourceFactory {

    @Override
    public FileSource create(Map<String, String> props) {

        MeteoFranceWcsSourceConfig config = MeteoFranceWcsSourceConfig.load(props);

        // --- setup
        MeteoFranceClient meteofranceClient = MeteoFranceClientFactory.create(props);

        MeteoFranceWcsService wcsService =
                new MeteoFranceWcsService(
                        meteofranceClient, config.getModelType(), config.getModel());

        return new MeteoFranceWcsSource(
                wcsService,
                config.getRequestedCoverageTitles(),
                config.getStartTime(),
                config.getEndTime());
    }
}
