package ngz.cloudconfidence.sparkmodels.models.intermediate;

import static org.apache.spark.sql.functions.callUDF;
import static org.apache.spark.sql.functions.coalesce;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.max;
import static org.apache.spark.sql.functions.row_number;

import ngz.cloudconfidence.sparkmodels.models.seed.SeedMeteoFranceStation;
import ngz.cloudconfidence.sparkmodels.models.staging.StgMeteoFrancePaquetSchema;
import ngz.markov.sparkmodel.execution.context.LookBackWindow;
import ngz.markov.sparkmodel.execution.context.PipelineContextUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.expressions.Window;

public class IntStationGridMappingProcessor {

    public static Dataset<IntStationGridMappingSchema> process(
            LookBackWindow lookBackWindow,
            Dataset<StgMeteoFrancePaquetSchema> stgMeteoFrancePaquet,
            Dataset<SeedMeteoFranceStation.Schema> seedMeteofranceStations) {

        Dataset<StgMeteoFrancePaquetSchema> timeFilteredData =
                PipelineContextUtils.filterByLookbackWindow(
                                stgMeteoFrancePaquet.toDF(), "validTime", lookBackWindow)
                        .as(Encoders.bean(StgMeteoFrancePaquetSchema.class));

        // 1. Extract distinct grid points from forecasts
        Dataset<Row> distinctGridPoints = extractDistinctGridPoints(stgMeteoFrancePaquet);

        // 2. Compute bounding box degree for spatial filtering
        double boundingBoxDegree = computeBoundingBoxDegree(distinctGridPoints);

        // 3. Prepare distinct stations reference
        Dataset<Row> formattedStations = prepareStationsData(seedMeteofranceStations);

        // 4. Enrich grid points with bounding box limits
        Dataset<Row> gridPointsWithBoundingBox =
                appendBoundingBoxLimits(distinctGridPoints, boundingBoxDegree);

        // 5. Map stations to their closest grid points
        Dataset<Row> closestGridMapping =
                mapClosestGridPoints(formattedStations, gridPointsWithBoundingBox);

        // 6. Select final schema and map to target Bean
        return finalizeDataset(closestGridMapping, boundingBoxDegree);
    }

    private static Dataset<Row> extractDistinctGridPoints(
            Dataset<StgMeteoFrancePaquetSchema> forecastData) {
        return forecastData
                .select(
                        col("model"),
                        col("latitude").alias("gridLatitude"),
                        col("longitude").alias("gridLongitude"),
                        col("grid"))
                .distinct();
    }

    private static double computeBoundingBoxDegree(Dataset<Row> gridPoints) {
        return gridPoints
                .agg(max(coalesce(col("grid"), lit(0.03))).alias("boxDeg"))
                .select("boxDeg")
                .first()
                .getDouble(0);
    }

    private static Dataset<Row> prepareStationsData(
            Dataset<SeedMeteoFranceStation.Schema> stationData) {
        return stationData
                .select(
                        col("stationId"),
                        col("stationName"),
                        col("latitude").cast("double").alias("stationLatitude"),
                        col("longitude").cast("double").alias("stationLongitude"))
                .distinct();
    }

    private static Dataset<Row> appendBoundingBoxLimits(
            Dataset<Row> gridPoints, double boundingBoxDegree) {
        return gridPoints
                .withColumn("minLat", col("gridLatitude").minus(boundingBoxDegree))
                .withColumn("maxLat", col("gridLatitude").plus(boundingBoxDegree))
                .withColumn("minLon", col("gridLongitude").minus(boundingBoxDegree))
                .withColumn("maxLon", col("gridLongitude").plus(boundingBoxDegree));
    }

    private static Dataset<Row> mapClosestGridPoints(
            Dataset<Row> stations, Dataset<Row> gridPointsWithBounds) {
        return stations.join(
                        gridPointsWithBounds,
                        // Bounding box pre-filter for performance
                        col("stationLatitude")
                                .geq(col("minLat"))
                                .and(col("stationLatitude").leq(col("maxLat")))
                                .and(col("stationLongitude").geq(col("minLon")))
                                .and(col("stationLongitude").leq(col("maxLon"))))
                // Calculate actual distance using Haversine formula
                .withColumn(
                        "distanceKm",
                        callUDF(
                                "haversineKm",
                                col("stationLatitude"),
                                col("stationLongitude"),
                                col("gridLatitude"),
                                col("gridLongitude")))
                // Rank grid points by distance for each station + model + grid
                .withColumn(
                        "distanceRank",
                        row_number()
                                .over(
                                        Window.partitionBy("stationId", "model", "grid")
                                                .orderBy(col("distanceKm").asc())))
                // Keep only the closest point
                .filter(col("distanceRank").equalTo(1))
                .drop("distanceRank", "distanceKm", "minLat", "maxLat", "minLon", "maxLon");
    }

    private static Dataset<IntStationGridMappingSchema> finalizeDataset(
            Dataset<Row> mappedData, double boundingBoxDegree) {
        return mappedData
                .select(
                        col("stationId"),
                        col("stationName"),
                        col("stationLatitude"),
                        col("stationLongitude"),
                        col("model"),
                        col("grid"),
                        col("gridLatitude"),
                        col("gridLongitude"),
                        lit(boundingBoxDegree).alias("boxDeg"))
                .as(Encoders.bean(IntStationGridMappingSchema.class));
    }
}
