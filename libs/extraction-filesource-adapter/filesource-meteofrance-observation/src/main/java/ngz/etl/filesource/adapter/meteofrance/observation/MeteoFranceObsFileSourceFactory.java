package ngz.etl.filesource.adapter.meteofrance.observation;

import java.util.Map;
import ngz.extraction.core.source.FileSource;
import ngz.extraction.core.source.FileSourceFactory;
import ngz.meteofrance.core.client.MeteoFranceClient;
import ngz.meteofrance.core.client.MeteoFranceClientConfig;
import ngz.meteofrance.core.client.MeteoFranceClientFactory;
import ngz.meteofrance.observation.service.StationService;
import ngz.meteofrance.observation.service.StationServiceFactory;

public class MeteoFranceObsFileSourceFactory implements FileSourceFactory {

    @Override
    public FileSource create(Map<String, String> props) {
        // configuration generic
        MeteoFranceObsFileSourceConfig config = MeteoFranceObsFileSourceConfig.fromMap(props);

        // client
        MeteoFranceClientConfig clientConfig =
                MeteoFranceClientConfig.builder(config.getApplicationId()).build();

        MeteoFranceClient client = MeteoFranceClientFactory.create(clientConfig);

        // obs(client)
        StationService service = StationServiceFactory.create(client);

        return new MeteoFranceObsFileSource(service, config.getDepartmentIds(), config.getFormat());
    }
}
