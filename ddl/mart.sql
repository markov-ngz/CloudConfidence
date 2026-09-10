CREATE DATABASE rest.mart;

USE DATABASe rest.mart ;

CREATE TABLE mart.mart_forecast_error_over_time (
                                               forecastModel STRING,
                                               stationId STRING,
                                               stationName STRING,
                                               time STRING,
                                               variable STRING,
                                               unit STRING,
                                               avgForecastError DOUBLE,
                                               avgObservationValue DOUBLE,
                                               techUpdatedAt TIMESTAMP
)
    USING iceberg ;

ALTER TABLE rest.mart.mart_forecast_error_over_time SET TBLPROPERTIES (
    'write.wap.enabled'='true'
    );

-- Statement to check the property is correctly set
ALTER TABLE rest.mart.mart_forecast_error_over_time CREATE BRANCH IF NOT EXISTS test_branch_123 ;

ALTER TABLE rest.mart.mart_forecast_error_over_time DROP BRANCH IF EXISTS test_branch_123 ;


SELECT * FROM mart.mart_forecast_error_over_time  ;