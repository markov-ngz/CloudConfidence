package ngz.cloudconfidence.convertgrib2;

import java.util.List;
import ngz.cloudconfidence.convertgrib2.airflow.XcomUtils;
import ngz.cloudconfidence.convertgrib2.configuration.Configuration;
import ngz.cloudconfidence.convertgrib2.configuration.ConfigurationLoader;
import ngz.cloudconfidence.convertgrib2.parquetconverter.GribParquetConverter;
import ngz.cloudconfidence.convertgrib2.parquetconverter.GribParquetConverterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {

        // Configuration / Arguments
        Configuration config = ConfigurationLoader.load(args);

        // Bootstrapping
        GribParquetConverter converter =
                GribParquetConverterFactory.create(
                        config.getHadoopConfig(), config.getJobId(), config.getOutputDir());

        try {

            List<ProcessResult> results = converter.convertMultiple(config.getSunkFiles());

            ConversionReport report = ConversionReport.fromProcessResults(results);

            XcomUtils.pushXcom(report, true, config.getXcomPath());

            System.exit(0);

        } catch (Exception e) {

            LOG.error("Critical application failure", e);

            XcomUtils.pushXcom(
                    List.of(ProcessResult.failure("APPLICATION", e.getMessage())),
                    false,
                    config.getXcomPath());

            System.exit(1);
        }
    }
}
