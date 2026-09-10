package ngz.cloudconfidence.convertgrib2.grib2;

import ngz.cloudconfidence.convertgrib2.grib2.exception.DatasetProcessingException;

@FunctionalInterface
public interface GridDatasetProcessor {
    /** Processes a grid dataset in a streaming fashion without leaking file handles. */
    void process(String filepath, GridRecordConsumer consumer) throws DatasetProcessingException;
}
