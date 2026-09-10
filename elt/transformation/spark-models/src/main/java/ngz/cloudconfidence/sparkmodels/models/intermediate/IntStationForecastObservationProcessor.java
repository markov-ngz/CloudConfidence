package ngz.cloudconfidence.sparkmodels.models.intermediate;

import static org.apache.spark.sql.functions.broadcast;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.to_timestamp;

import ngz.cloudconfidence.sparkmodels.models.staging.StgMeteoFrancePaquetSchema;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

public class IntStationForecastObservationProcessor {

    public static Dataset<IntStationForecastObservationSchema> process(
            Dataset<IntUnpivotObservationSchema> intUnpivotObservation,
            Dataset<StgMeteoFrancePaquetSchema> stgMeteoFrancePaquet,
            Dataset<IntStationGridMappingSchema> intStationGridMapping) {

        // 1. Enrich forecasts with station data via spatial mapping
        Dataset<Row> forecastsWithStations =
                enrichForecastsWithStationMapping(stgMeteoFrancePaquet, intStationGridMapping);

        // 2. Join forecast data with actual observations
        Dataset<Row> matchedForecastsAndObservations =
                matchForecastsWithObservations(forecastsWithStations, intUnpivotObservation);

        // 3. Compute error metrics, select final schema, and map to target Bean
        return calculateErrorsAndFinalizeDataset(matchedForecastsAndObservations);
    }

    private static Dataset<Row> enrichForecastsWithStationMapping(
            Dataset<StgMeteoFrancePaquetSchema> forecasts,
            Dataset<IntStationGridMappingSchema> gridMapping) {

        return forecasts
                .as("forecast")
                .join(
                        // Broadcast mapping for performance as it is likely small
                        broadcast(gridMapping.as("mapping")),
                        col("forecast.latitude")
                                .equalTo(col("mapping.gridLatitude"))
                                .and(
                                        col("forecast.longitude")
                                                .equalTo(col("mapping.gridLongitude"))),
                        "inner")
                .drop(
                        col("mapping.model"),
                        col("mapping.grid"),
                        col("mapping.gridLatitude"),
                        col("mapping.gridLongitude"));
    }

    private static Dataset<Row> matchForecastsWithObservations(
            Dataset<Row> forecastsWithStations, Dataset<IntUnpivotObservationSchema> observations) {

        return forecastsWithStations
                .as("station_forecast")
                .join(
                        observations.as("obs"),
                        col("station_forecast.stationId")
                                .equalTo(col("obs.stationId"))
                                .and(
                                        to_timestamp(col("station_forecast.validTime"))
                                                .equalTo(
                                                        col("obs.observationTime")
                                                                .cast("timestamp")))
                                .and(col("station_forecast.variable").equalTo(col("obs.variable"))),
                        "inner");
    }

    private static Dataset<IntStationForecastObservationSchema> calculateErrorsAndFinalizeDataset(
            Dataset<Row> matchedData) {
        return matchedData
                // Compute the difference between observation and forecast
                .withColumn("forecastError", col("obs.value").minus(col("station_forecast.value")))
                .select(
                        col("station_forecast.model").alias("forecastModel"),
                        col("station_forecast.stationId"),
                        col("station_forecast.stationName"),
                        col("station_forecast.stationLatitude"),
                        col("station_forecast.stationLongitude"),
                        col("station_forecast.meteofranceReferenceTime").alias("referenceTime"),
                        col("station_forecast.meteofranceTime").alias("time"),
                        col("obs.observationTime"),
                        col("station_forecast.variable").alias("variable"),
                        col("station_forecast.value").alias("forecastValue"),
                        col("station_forecast.unit").alias("forecastUnit"),
                        col("forecastError"),
                        col("obs.value").alias("observationValue"),
                        col("obs.unit").alias("observationUnit"))
                .as(Encoders.bean(IntStationForecastObservationSchema.class));
    }
}
