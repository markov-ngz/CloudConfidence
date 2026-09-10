package ngz.cloudconfidence.sparkmodels.application;

import ngz.markov.sparkmodel.execution.ExecutionObserver;
import ngz.markov.sparkmodel.execution.ModelExecutionResult;
import ngz.markov.sparkmodel.model.SparkModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple logging on model execution, it could be removed and have metrics sent to proper external
 * system.
 */
public class LoggingExecutionObserver implements ExecutionObserver {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingExecutionObserver.class);

    @Override
    public void onModelStart(SparkModel model) {
        LOG.info("Starting model: {}", model.getClass().getSimpleName());
    }

    @Override
    public void onModelSuccess(ModelExecutionResult result) {

        LOG.info(
                "Finished model: {} in {} ms | Written: {} rows",
                result.model().getClass().getSimpleName(),
                result.durationMs(),
                result.metrics() != null ? result.metrics().recordsWritten() : 0);
    }

    @Override
    public void onModelFailure(SparkModel model, Throwable throwable) {
        LOG.error(
                "Failed model: {} - Error: {}",
                model.getClass().getSimpleName(),
                throwable.getMessage());
    }
}
