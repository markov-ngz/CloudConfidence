package ngz.cloudconfidence.sparkmodels.models.staging;

import static org.apache.spark.sql.functions.coalesce;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.lower;
import static org.apache.spark.sql.functions.to_timestamp;
import static org.apache.spark.sql.functions.when;

import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedUnit;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedVariable;
import ngz.cloudconfidence.sparkmodels.models.source.SourceMeteoFrancePaquet;
import ngz.markov.sparkmodel.execution.context.LookBackWindow;
import ngz.markov.sparkmodel.execution.context.PipelineContextUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

public class StgMeteoFrancePaquetProcessor {

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static Dataset<StgMeteoFrancePaquetSchema> process(
            Dataset<SourceMeteoFrancePaquet.Schema> sourceMeteoFrancePaquet,
            Dataset<SeedStandardizedVariable.Schema> seedStandardizedVariable,
            Dataset<SeedStandardizedUnit.Schema> seedStandardizedUnit,
            LookBackWindow lookBackWindow) {

        // 1. Align schema (grouping renames early)
        Dataset<Row> schemaAlignedData = alignSchema(sourceMeteoFrancePaquet);

        // 2. Format temporal fields
        Dataset<Row> timeFormattedData = castTimestamps(schemaAlignedData);

        // 3. Filter data by retention/look-back window
        Dataset<Row> timeFilteredData =
                PipelineContextUtils.filterByLookbackWindow(
                        schemaAlignedData.toDF(), "validTime", lookBackWindow);

        // 4. Enrich & Standardize Variables
        Dataset<Row> variableStandardizedData =
                standardizeVariables(timeFilteredData, seedStandardizedVariable);

        // 5. Enrich & Standardize Units and compute true values
        Dataset<Row> unitStandardizedData =
                standardizeUnitsAndValues(variableStandardizedData, seedStandardizedUnit);

        // 6. Derive missing business features
        Dataset<Row> featureEnrichedData = deriveModelAndGridFeatures(unitStandardizedData);

        // 7. Select final schema and map to target Bean
        return finalizeDataset(featureEnrichedData);
    }

    private static Dataset<Row> alignSchema(Dataset<SourceMeteoFrancePaquet.Schema> data) {
        return data.withColumnRenamed("lat", "latitude").withColumnRenamed("lon", "longitude");
    }

    private static Dataset<Row> castTimestamps(Dataset<Row> data) {
        return data.withColumn(
                        "meteofranceReferenceTime",
                        to_timestamp(col("meteofranceReferenceTime"), TIMESTAMP_FORMAT))
                .withColumn("validTime", to_timestamp(col("validTime"), TIMESTAMP_FORMAT))
                .withColumn("startTime", to_timestamp(col("startTime"), TIMESTAMP_FORMAT))
                .withColumn("endTime", to_timestamp(col("endTime"), TIMESTAMP_FORMAT));
    }

    private static Dataset<Row> standardizeVariables(
            Dataset<Row> data, Dataset<SeedStandardizedVariable.Schema> seedVariables) {
        return data.join(
                        seedVariables,
                        data.col("variable").equalTo(seedVariables.col("variableName")),
                        "left")
                .withColumn("variable", coalesce(col("standardizedVariableName"), col("variable")))
                .drop("variableName", "standardizedVariableName");
    }

    private static Dataset<Row> standardizeUnitsAndValues(
            Dataset<Row> data, Dataset<SeedStandardizedUnit.Schema> seedUnits) {
        return data.join(
                        seedUnits, data.col("unit").equalTo(seedUnits.col("unitShortName")), "left")
                .withColumn(
                        "value",
                        col("value")
                                .cast("double")
                                .multiply(coalesce(col("coefficient").cast("double"), lit(1.0)))
                                .plus(coalesce(col("offset").cast("double"), lit(0.0))))
                .withColumn("unit", coalesce(col("standardizedUnit"), col("unit")))
                .drop("unitShortName", "standardizedUnit", "coefficient", "offset");
    }

    private static Dataset<Row> deriveModelAndGridFeatures(Dataset<Row> data) {
        return data.withColumn(
                        "model",
                        when(lower(col("meteofranceModel")).contains("arome"), lit("AROME"))
                                .when(
                                        lower(col("meteofranceModel")).contains("arpege"),
                                        lit("ARPEGE"))
                                .otherwise(lit(null).cast("string")))
                .withColumn("grid", col("meteofranceGrid").cast("double"));
    }

    private static Dataset<StgMeteoFrancePaquetSchema> finalizeDataset(Dataset<Row> data) {
        return data.where(col("value").isNotNull())
                .select(
                        "meteofrancePrevinum",
                        "meteofranceModel",
                        "meteofranceGrid",
                        "meteofrancePackage",
                        "meteofranceReferenceTime",
                        "meteofranceTime",
                        "validTime",
                        "startTime",
                        "endTime",
                        "levelType",
                        "levelValue",
                        "isLatLon",
                        "projection",
                        "gridX",
                        "gridY",
                        "latitude",
                        "longitude",
                        "variable",
                        "unit",
                        "value",
                        "model",
                        "grid")
                .as(Encoders.bean(StgMeteoFrancePaquetSchema.class));
    }
}
