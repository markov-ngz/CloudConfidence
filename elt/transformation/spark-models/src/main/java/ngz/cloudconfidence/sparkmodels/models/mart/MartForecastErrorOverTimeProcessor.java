package ngz.cloudconfidence.sparkmodels.models.mart;

import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.nullif;
import static org.apache.spark.sql.functions.sum;

import ngz.cloudconfidence.sparkmodels.models.intermediate.IntAggForecastErrorOverTimeSchema;
import ngz.cloudconfidence.sparkmodels.models.intermediate.IntStationForecastObservationSchema;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

public class MartForecastErrorOverTimeProcessor {

    /** Incremental processing using the pre-aggregated intermediate table. */
    public static Dataset<MartForecastErrorOverTime> process(
            Dataset<IntAggForecastErrorOverTimeSchema> intAggForecastErrorOverTime) {

        // 1. Compute global averages from pre-aggregated sums
        Dataset<Row> computedAverages = computeIncrementalAverages(intAggForecastErrorOverTime);

        // 2. Format schema, add technical metadata, and map to target Bean
        return finalizeDataset(computedAverages);
    }

    /** Full recalculation processing from the raw station forecast observations. */
    public static Dataset<MartForecastErrorOverTime> processFull(
            Dataset<IntStationForecastObservationSchema> intStationForecastObservation) {

        // 1. Group raw data and compute exact averages
        Dataset<Row> computedAverages = computeFullAverages(intStationForecastObservation);

        // 2. Format schema, add technical metadata, and map to target Bean
        return finalizeDataset(computedAverages);
    }

    private static Dataset<Row> computeIncrementalAverages(
            Dataset<IntAggForecastErrorOverTimeSchema> data) {
        return data.groupBy("forecastModel", "stationId", "stationName", "variable", "time", "unit")
                .agg(
                        sum("sumForecastError")
                                .divide(nullif(sum("countRecords"), lit(0)))
                                .alias("avgForecastError"),
                        sum("sumObservationValue")
                                .divide(nullif(sum("countRecords"), lit(0)))
                                .alias("avgObservationValue"));
    }

    private static Dataset<Row> computeFullAverages(
            Dataset<IntStationForecastObservationSchema> data) {
        return data.groupBy(
                        "forecastModel",
                        "stationId",
                        "stationName",
                        "variable",
                        "time",
                        "observationUnit")
                .agg(
                        avg("forecastError").alias("avgForecastError"),
                        avg("observationValue").alias("avgObservationValue"))
                // Align column name with the incremental approach schema
                .withColumnRenamed("observationUnit", "unit");
    }

    private static Dataset<MartForecastErrorOverTime> finalizeDataset(Dataset<Row> aggregatedData) {
        return aggregatedData
                .select(
                        col("forecastModel"),
                        col("stationId"),
                        col("stationName"),
                        col("variable"),
                        col("unit"),
                        col("time"),
                        col("avgForecastError"),
                        col("avgObservationValue"))
                // Add technical tracking columns
                .withColumn("techUpdatedAt", current_timestamp())
                // Ensure deterministic ordering
                .orderBy(
                        col("forecastModel"),
                        col("stationId"),
                        col("stationName"),
                        col("variable"),
                        col("unit"),
                        col("time"))
                .as(Encoders.bean(MartForecastErrorOverTime.class));
    }
}
