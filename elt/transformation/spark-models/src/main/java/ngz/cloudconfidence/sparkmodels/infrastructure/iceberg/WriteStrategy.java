package ngz.cloudconfidence.sparkmodels.infrastructure.iceberg;

public enum WriteStrategy {
    INSERT_IF_NOT_EXISTS,
    MERGE,
    APPEND,
    CREATE_OR_REPLACE
}
