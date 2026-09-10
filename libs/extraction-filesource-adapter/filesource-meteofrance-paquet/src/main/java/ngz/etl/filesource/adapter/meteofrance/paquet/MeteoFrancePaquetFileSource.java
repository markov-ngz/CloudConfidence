package ngz.etl.filesource.adapter.meteofrance.paquet;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ngz.extraction.core.model.FileInformation;
import ngz.extraction.core.source.FileSource;
import ngz.meteofrance.paquet.model.Paquet;
import ngz.meteofrance.paquet.model.ProductData;
import ngz.meteofrance.paquet.model.ProductInfo;
import ngz.meteofrance.paquet.service.PaquetService;
import ngz.meteofrance.paquet.service.ProductInfoService;

public class MeteoFrancePaquetFileSource implements FileSource {

    private final transient MeteoFrancePaquetFileSourceFactory factory =
            new MeteoFrancePaquetFileSourceFactory();

    private final ProductInfoService productInfoService;
    private final PaquetService paquetService;
    private final String previnum;
    private final String model;
    private final String grid;
    private final List<String> packageNames;
    private final Instant startTime;
    private final Instant endTime;

    public MeteoFrancePaquetFileSource(
            ProductInfoService productInfoService,
            PaquetService paquetService,
            String previnum,
            String model,
            String grid,
            List<String> packageNames,
            Instant startTime,
            Instant endTime) {
        this.productInfoService = productInfoService;
        this.paquetService = paquetService;
        this.previnum = previnum;
        this.model = model;
        this.grid = grid;
        this.packageNames = packageNames;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public List<FileInformation> listFiles() {

        List<FileInformation> fileInformations = new ArrayList<>();

        for (String packageName : packageNames) {

            Paquet paquet = this.paquetService.describePaquet(previnum, model, grid, packageName);

            for (Instant referenceTime : paquet.getReferenceTimes()) {

                // Check if between the specified time range
                if (!(referenceTime.isAfter(this.startTime)
                        && referenceTime.isBefore(this.endTime))) {
                    continue;
                }

                List<ProductInfo> productInfos =
                        productInfoService.getAvailableForecasts(
                                previnum, model, grid, packageName, referenceTime);

                for (ProductInfo productInfo : productInfos) {
                    FileInformation fileInfo =
                            FileInformation.builder()
                                    .id(buildFileInformationId(productInfo)) // location as id
                                    .location(productInfo.getLocation())
                                    .format(productInfo.getDataFormat())
                                    .metadata(buildMetadata(productInfo))
                                    .build();
                    fileInformations.add(fileInfo);
                }
            }
        }

        return fileInformations;
    }

    private String buildFileInformationId(ProductInfo productInfo) {
        return String.join(
                "_",
                productInfo.getModel(),
                productInfo.getGridId(),
                productInfo.getPackageName(),
                productInfo.getReferenceTime().toString(),
                String.valueOf(productInfo.getTime()));
    }

    private Map<String, String> buildMetadata(ProductInfo productInfo) {
        return Map.of(
                "previnum", productInfo.getPrevinum(),
                "model", productInfo.getModel(),
                "grid", productInfo.getGridId(),
                "package", productInfo.getPackageName(),
                "referenceTime", productInfo.getReferenceTime().toString(),
                "time", String.valueOf(productInfo.getTime()));
    }

    @Override
    public byte[] downloadFile(String fileLocation) {
        ProductData productData = this.productInfoService.downloadForecastDataByHref(fileLocation);
        return productData.getContent();
    }
}
