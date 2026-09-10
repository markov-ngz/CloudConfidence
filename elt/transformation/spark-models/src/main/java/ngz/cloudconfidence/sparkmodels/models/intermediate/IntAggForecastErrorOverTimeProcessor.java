package ngz.cloudconfidence.sparkmodels.models.intermediate;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.pow;
import static org.apache.spark.sql.functions.sum;

import ngz.markov.sparkmodel.execution.context.LookBackWindow;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

public class IntAggForecastErrorOverTimeProcessor {

    public static Dataset<IntAggForecastErrorOverTimeSchema> process(
            Dataset<IntStationForecastObservationSchema> intStationForecastObservation,
            LookBackWindow lookBackWindow) {

        // 1. Group data and compute aggregations
        Dataset<Row> aggregatedErrors = computeErrorAggregations(intStationForecastObservation);

        // 2. Format schema, add technical metadata, and map to target Bean
        return finalizeDataset(aggregatedErrors, lookBackWindow);
    }

    private static Dataset<Row> computeErrorAggregations(
            Dataset<IntStationForecastObservationSchema> data) {
        return data.groupBy(
                        "forecastModel",
                        "stationId",
                        "stationName",
                        "variable",
                        "time",
                        "observationUnit")
                .agg(
                        sum("forecastError").alias("sumForecastError"),
                        sum(pow("forecastError", 2)).alias("sumSquaredError"),
                        sum("observationValue").alias("sumObservationValue"),
                        count("*").alias("countRecords"));
    }

    private static Dataset<IntAggForecastErrorOverTimeSchema> finalizeDataset(
            Dataset<Row> aggregatedData, LookBackWindow lookBackWindow) {

        return aggregatedData
                .select(
                        col("forecastModel"),
                        col("stationId"),
                        col("stationName"),
                        col("variable"),
                        col("observationUnit").alias("unit"),
                        col("time"),
                        col("sumForecastError"),
                        col("sumSquaredError"),
                        col("sumObservationValue"),
                        col("countRecords"))
                // Add technical tracking columns
                .withColumn("techWindowStartTime", lit(lookBackWindow.startTime()))
                .withColumn("techWindowEndTime", lit(lookBackWindow.endTime()))
                .withColumn("techUpdatedAt", current_timestamp())
                // Ensure deterministic ordering
                .orderBy(
                        col("forecastModel"),
                        col("stationId"),
                        col("stationName"),
                        col("variable"),
                        col("unit"),
                        col("time"))
                .as(Encoders.bean(IntAggForecastErrorOverTimeSchema.class));
    }
}
