package ngz.cloudconfidence.sparkmodels;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.application.CloudConfidencePipelineFactory;
import ngz.cloudconfidence.sparkmodels.application.persistence.IcebergTableWriter;
import ngz.cloudconfidence.sparkmodels.application.persistence.TableWriterRegistry;
import ngz.cloudconfidence.sparkmodels.configuration.CloudConfidenceConfiguration;
import ngz.cloudconfidence.sparkmodels.infrastructure.airflow.XcomPusher;
import ngz.markov.sparkmodel.pipeline.Pipeline;
import ngz.markov.sparkmodel.pipeline.PipelineResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudConfidenceApplication {

    private static final Logger LOG = LoggerFactory.getLogger(CloudConfidenceApplication.class);

    public static void main(String[] args) {

        LOG.info("Starting process");

        CloudConfidenceConfiguration pipelineConfiguration =
                CloudConfidenceConfiguration.load(args);

        LOG.info("Configuration loaded successfully");

        LOG.info(pipelineConfiguration.toString());

        try {

            TableWriterRegistry.register(new IcebergTableWriter());
            Pipeline pipeline = CloudConfidencePipelineFactory.create(pipelineConfiguration);

            PipelineResult result = pipeline.run();

            XcomPusher.pushXcom(result, true, pipelineConfiguration.getXcomPath());

            LOG.info("Process completed successfully exiting ...");

            System.exit(0);

        } catch (Exception e) {
            LOG.error("Unexpected critical application failure", e);

            XcomPusher.pushXcom(
                    new PipelineResult(List.of(), false),
                    false,
                    pipelineConfiguration.getXcomPath());

            System.exit(1);
        }
    }
}
