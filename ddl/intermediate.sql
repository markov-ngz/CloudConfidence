USE rest ;

CREATE DATABASE rest.intermediate ;

CREATE OR REPLACE TABLE intermediate.int_agg_forecast_error_over_time (
       stationId STRING,
       stationName STRING,
       forecastModel STRING,
       time STRING,
       variable STRING,
       unit STRING,
       sumForecastError DOUBLE,
       sumSquaredError DOUBLE,
       sumObservationValue DOUBLE,
       countRecords DOUBLE,
       techWindowStartTime TIMESTAMP,
       techWindowEndTime TIMESTAMP,
       techUpdatedAt TIMESTAMP
)
    USING ICEBERG;

CREATE OR REPLACE TABLE intermediate.int_station_forecast_observation (
   forecastModel STRING,
   stationId STRING,
   stationName STRING,
   stationLatitude DOUBLE,
   stationLongitude DOUBLE,
   referenceTime STRING,
   time STRING,
   observationTime TIMESTAMP,
   variable STRING,
   forecastValue DOUBLE,
   forecastUnit STRING,
   forecastError DOUBLE,
   observationValue DOUBLE,
   observationUnit STRING
)
    USING ICEBERG;