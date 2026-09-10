package ngz.cloudconfidence.sparkmodels.application;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.configuration.CloudConfidenceConfiguration;
import ngz.cloudconfidence.sparkmodels.models.CloudConfidenceModelCatalog;
import ngz.markov.sparkmodel.execution.ExecutionObserver;
import ngz.markov.sparkmodel.execution.context.PipelineContextFactory;
import ngz.markov.sparkmodel.pipeline.Pipeline;
import ngz.markov.sparkmodel.registry.ModelRegistry;
import ngz.markov.sparkmodel.registry.ModelRegistryFactory;
import ngz.markov.sparkmodel.selection.SelectorQuery;
import ngz.markov.sparkmodel.selection.SelectorQueryFactory;

public class CloudConfidencePipelineFactory {

    public static Pipeline create(CloudConfidenceConfiguration cloudConfidenceConfiguration) {

        PipelineContextFactory pipelineContextFactory =
                new CloudConfidencePipelineContextFactory(
                        cloudConfidenceConfiguration.getPipelineContextPath(),
                        cloudConfidenceConfiguration.getPipelineProps(),
                        cloudConfidenceConfiguration.getPolarisCredentials());

        ModelRegistry modelRegistry =
                ModelRegistryFactory.createDefault(
                        cloudConfidenceConfiguration.getModelRegistryPath(),
                        CloudConfidenceModelCatalog.MODELZ);

        SelectorQuery selectorQuery =
                SelectorQueryFactory.createFromString(
                        cloudConfidenceConfiguration.getSelector(),
                        cloudConfidenceConfiguration.getSelectorDelimiter());

        ExecutionObserver observer = new LoggingExecutionObserver();

        return new Pipeline(
                modelRegistry, pipelineContextFactory, selectorQuery, List.of(observer));
    }
}
