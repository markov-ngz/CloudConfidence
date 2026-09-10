package ngz.cloudconfidence.sparkmodels.models.intermediate;

import static org.apache.spark.sql.functions.coalesce;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.lit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedMeteoFranceObservationVariable;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedUnit;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedVariable;
import ngz.cloudconfidence.sparkmodels.models.staging.StgMeteoFranceObservationSchema;
import ngz.markov.sparkmodel.execution.context.LookBackWindow;
import ngz.markov.sparkmodel.execution.context.PipelineContextUtils;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;

public class IntUnpivotObservationProcessor {

    public static Dataset<IntUnpivotObservationSchema> process(
            LookBackWindow lookBackWindow,
            Dataset<StgMeteoFranceObservationSchema> stgMeteoFranceObservation,
            Dataset<SeedMeteoFranceObservationVariable.Schema> seedMeteofranceObservationVariable,
            Dataset<SeedStandardizedVariable.Schema> seedStandardizedVariable,
            Dataset<SeedStandardizedUnit.Schema> seedStandardizedUnit) {

        // 0. Filter by time
        Dataset<StgMeteoFranceObservationSchema> timeFilteredData =
                PipelineContextUtils.filterByLookbackWindow(
                                stgMeteoFranceObservation.toDF(), "observationTime", lookBackWindow)
                        .as(Encoders.bean(StgMeteoFranceObservationSchema.class));

        // 1. Build variable reference to map standardized names to units
        Dataset<Row> variableReference =
                buildVariableReference(
                        seedMeteofranceObservationVariable, seedStandardizedVariable);

        // 2. Identify which value columns are actually present in the dataset
        Set<String> presentValueColumns =
                extractPresentValueColumns(variableReference, timeFilteredData.columns());

        // 3. Unpivot the dataset (transform columns to rows)
        Dataset<Row> unpivotedObservations =
                unpivotObservations(timeFilteredData, presentValueColumns);

        // 4. Enrich data with standardized units and calculate true values
        Dataset<Row> unitStandardizedData =
                enrichWithStandardizedUnits(
                        unpivotedObservations, variableReference, seedStandardizedUnit);

        // 5. Select final schema and map to target Bean
        return finalizeDataset(unitStandardizedData);
    }

    private static Dataset<Row> buildVariableReference(
            Dataset<SeedMeteoFranceObservationVariable.Schema> observationVariables,
            Dataset<SeedStandardizedVariable.Schema> standardizedVariables) {
        return observationVariables
                .join(standardizedVariables, "variableName")
                .select(
                        col("standardizedVariableName").alias("variable"),
                        col("variableUnit").alias("unit"));
    }

    private static Set<String> extractPresentValueColumns(
            Dataset<Row> variableReference, String[] observationColumns) {
        Set<String> allObservationColumns = new HashSet<>(Arrays.asList(observationColumns));

        return variableReference.select("variable").as(Encoders.STRING()).collectAsList().stream()
                .filter(allObservationColumns::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Dataset<Row> unpivotObservations(
            Dataset<StgMeteoFranceObservationSchema> observations, Set<String> valueColumnNames) {

        Column[] idColumns =
                Stream.of("latitude", "longitude", "stationId", "stationName", "observationTime")
                        .map(functions::col)
                        .toArray(Column[]::new);

        Column[] valueColumns =
                valueColumnNames.stream().map(functions::col).toArray(Column[]::new);

        return observations.unpivot(idColumns, valueColumns, "variable", "value");
    }

    private static Dataset<Row> enrichWithStandardizedUnits(
            Dataset<Row> unpivotedData,
            Dataset<Row> variableReference,
            Dataset<SeedStandardizedUnit.Schema> seedStandardizedUnits) {

        // Attach base units from the reference mapping
        Dataset<Row> dataWithBaseUnits = unpivotedData.join(variableReference, "variable", "left");

        // Apply standardized unit conversions and offsets
        return dataWithBaseUnits
                .join(
                        seedStandardizedUnits,
                        dataWithBaseUnits
                                .col("unit")
                                .equalTo(seedStandardizedUnits.col("unitShortName")),
                        "left")
                .withColumn(
                        "value",
                        col("value")
                                .cast("double")
                                .multiply(coalesce(col("coefficient").cast("double"), lit(1.0)))
                                .plus(coalesce(col("offset").cast("double"), lit(0.0))))
                .withColumn("unit", coalesce(col("standardizedUnit"), col("unit")))
                .drop("unitShortName", "standardizedUnit", "coefficient", "offset");
    }

    private static Dataset<IntUnpivotObservationSchema> finalizeDataset(Dataset<Row> data) {
        return data.where(col("value").isNotNull())
                .select(
                        "variable",
                        "latitude",
                        "longitude",
                        "stationId",
                        "stationName",
                        "observationTime",
                        "value",
                        "unit")
                .as(Encoders.bean(IntUnpivotObservationSchema.class));
    }
}
