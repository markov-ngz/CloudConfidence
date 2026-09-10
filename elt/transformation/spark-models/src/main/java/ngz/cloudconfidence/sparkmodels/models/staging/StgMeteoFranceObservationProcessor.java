package ngz.cloudconfidence.sparkmodels.models.staging;

import static org.apache.spark.sql.functions.col;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedMeteoFranceObservationVariable;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedMeteoFranceStation;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedVariable;
import ngz.cloudconfidence.sparkmodels.models.source.SourceMeteoFranceObservation;
import ngz.markov.sparkmodel.execution.context.LookBackWindow;
import ngz.markov.sparkmodel.execution.context.PipelineContextUtils;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import scala.Tuple2;

public class StgMeteoFranceObservationProcessor {

    public static Dataset<StgMeteoFranceObservationSchema> process(
            Dataset<SourceMeteoFranceObservation.Schema> sourceMeteoFranceObservation,
            Dataset<SeedMeteoFranceObservationVariable.Schema> meteofranceObservationVariable,
            Dataset<SeedMeteoFranceStation.Schema> meteofranceStations,
            Dataset<SeedStandardizedVariable.Schema> standardizedVariable,
            LookBackWindow lookBackWindow) {

        // 1. Filter data by window to reduce processing volume
        Dataset<Row> timeFilteredData =
                PipelineContextUtils.filterByLookbackWindow(
                        sourceMeteoFranceObservation.toDF(), "observationTime", lookBackWindow);

        // 2. Build map for standardizing column names
        Map<String, String> columnRenameMap =
                buildRenameMap(meteofranceObservationVariable, standardizedVariable);

        // 3. Apply standard names to data
        Dataset<Row> renamedData = applyColumnRenames(timeFilteredData, columnRenameMap);

        // 4. Enrich observations with reference station data
        Dataset<Row> stationEnrichedData = enrichWithStationData(renamedData, meteofranceStations);

        // 5. Select final schema and map to target Bean
        return finalizeDataset(stationEnrichedData);
    }

    private static Map<String, String> buildRenameMap(
            Dataset<SeedMeteoFranceObservationVariable.Schema> observationVariables,
            Dataset<SeedStandardizedVariable.Schema> standardizedVariables) {

        // Match raw variable names to standardized names
        Dataset<Row> variableReference =
                observationVariables
                        .join(standardizedVariables, "variableName")
                        .select(col("variableName"), col("standardizedVariableName"));

        // Build dynamic map
        Map<String, String> renameMap =
                variableReference
                        .as(Encoders.tuple(Encoders.STRING(), Encoders.STRING()))
                        .collectAsList()
                        .stream()
                        .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));

        // Add static coordinate renames
        renameMap.put("lat", "latitude");
        renameMap.put("long", "longitude");

        return renameMap;
    }

    private static Dataset<Row> applyColumnRenames(
            Dataset<Row> data, Map<String, String> renameMap) {
        Column[] renamedColumns =
                Arrays.stream(data.columns())
                        .map(c -> col(c).alias(renameMap.getOrDefault(c, c)))
                        .toArray(Column[]::new);

        return data.select(renamedColumns);
    }

    private static Dataset<Row> enrichWithStationData(
            Dataset<Row> data, Dataset<SeedMeteoFranceStation.Schema> meteofranceStations) {

        Dataset<Row> stationsReference =
                meteofranceStations.select(col("stationId"), col("stationName"));

        return data.as("obs")
                .join(
                        stationsReference.as("station"),
                        data.col("inseeLocationId").equalTo(stationsReference.col("stationId")),
                        "left")
                // Standardize station ID column name post-join
                .withColumnRenamed("inseeLocationId", "stationId")
                .drop(stationsReference.col("stationId"));
    }

    private static Dataset<StgMeteoFranceObservationSchema> finalizeDataset(Dataset<Row> data) {
        return data.select(
                        col("latitude"),
                        col("longitude"),
                        col("stationId"),
                        col("productionTime"),
                        col("ingestionTime"),
                        col("observationTime"),
                        col("airTemperature"),
                        col("dewPointTemperature"),
                        col("maxAirTemperature"),
                        col("minAirTemperature"),
                        col("relativeHumidity"),
                        col("maxRelativeHumidity"),
                        col("minRelativeHumidity"),
                        col("windDirection"),
                        col("windSpeed"),
                        col("maxWindDirection"),
                        col("maxWindSpeed"),
                        col("gustDirection"),
                        col("maxWindGust"),
                        col("hourlyPrecipitation"),
                        col("soilTemperature10cm"),
                        col("soilTemperature20cm"),
                        col("soilTemperature50cm"),
                        col("soilTemperature100cm"),
                        col("groundStateCode"),
                        col("snowDepth"),
                        col("horizontalVisibility"),
                        col("totalCloudCover"),
                        col("sunshineDuration"),
                        col("hourlyGlobalRadiation"),
                        col("stationPressure"),
                        col("seaLevelPressure"),
                        col("stationName"))
                .as(Encoders.bean(StgMeteoFranceObservationSchema.class));
    }
}
