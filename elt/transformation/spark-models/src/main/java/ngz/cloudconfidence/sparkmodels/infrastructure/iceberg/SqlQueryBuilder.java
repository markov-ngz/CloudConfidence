package ngz.cloudconfidence.sparkmodels.infrastructure.iceberg;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.List;
import ngz.cloudconfidence.sparkmodels.infrastructure.iceberg.exception.UnsupportedStrategyException;

public class SqlQueryBuilder {

    private SqlQueryBuilder() {}

    public static String build(
            WriteStrategy strategy,
            String targetTable,
            String sourceView,
            List<String> primaryKeys,
            String[] columns) {
        String joinCondition = buildJoinCondition(primaryKeys);
        String insertCols = String.join(", ", columns);
        String insertVals = Arrays.stream(columns).map(c -> "source." + c).collect(joining(", "));
        String updateSetClause =
                Arrays.stream(columns)
                        .map(c -> "target.%s = source.%s".formatted(c, c))
                        .collect(joining(", "));

        return switch (strategy) {
            case APPEND ->
                    """
                    INSERT INTO %s (%s)
                    SELECT %s FROM %s
                    """
                            .formatted(targetTable, insertCols, insertVals, sourceView);

            case INSERT_IF_NOT_EXISTS ->
                    """
                    MERGE INTO %s AS target
                    USING %s AS source
                    ON %s
                    WHEN NOT MATCHED THEN
                      INSERT (%s) VALUES (%s)
                    """
                            .formatted(
                                    targetTable, sourceView, joinCondition, insertCols, insertVals);

            case MERGE ->
                    """
                    MERGE INTO %s AS target
                    USING %s AS source
                    ON %s
                    WHEN MATCHED THEN
                      UPDATE SET %s
                    WHEN NOT MATCHED THEN
                      INSERT (%s) VALUES (%s)
                    """
                            .formatted(
                                    targetTable,
                                    sourceView,
                                    joinCondition,
                                    updateSetClause,
                                    insertCols,
                                    insertVals);

            default ->
                    throw new UnsupportedStrategyException(
                            "Strategy not supported: " + strategy.name());
        };
    }

    private static String buildJoinCondition(List<String> primaryKeys) {
        if (primaryKeys.isEmpty()) {
            return "";
        }

        return primaryKeys.stream()
                .map(k -> "target.%s = source.%s".formatted(k, k))
                .collect(joining(" AND "));
    }
}
