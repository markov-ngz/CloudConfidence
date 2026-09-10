package ngz.etl.filesource.adapter.meteofrance.paquet;

import java.util.Map;
import ngz.extraction.core.source.FileSource;
import ngz.extraction.core.source.FileSourceFactory;
import ngz.meteofrance.core.client.MeteoFranceClient;
import ngz.meteofrance.core.client.MeteoFranceClientConfig;
import ngz.meteofrance.core.client.MeteoFranceClientFactory;
import ngz.meteofrance.paquet.service.PaquetService;
import ngz.meteofrance.paquet.service.PaquetServiceFactory;
import ngz.meteofrance.paquet.service.ProductInfoService;
import ngz.meteofrance.paquet.service.ProductInfoServiceFactory;

public class MeteoFrancePaquetFileSourceFactory implements FileSourceFactory {

    @Override
    public FileSource create(Map<String, String> props) {

        // Source application
        MeteoFrancePaquetFileSourceConfig config = MeteoFrancePaquetFileSourceConfig.fromMap(props);

        // client
        MeteoFranceClientConfig clientConfig =
                MeteoFranceClientConfig.builder(config.getApplicationId()).build();
        MeteoFranceClient client = MeteoFranceClientFactory.create(clientConfig);

        // service(client)
        ProductInfoService productInfoService = ProductInfoServiceFactory.create(client);
        PaquetService paquetService = PaquetServiceFactory.create(client);

        return new MeteoFrancePaquetFileSource(
                productInfoService,
                paquetService,
                config.getPrevinum(),
                config.getModel(),
                config.getGrid(),
                config.getPackageNames(),
                config.getStartTime(),
                config.getEndTime());
    }
}
