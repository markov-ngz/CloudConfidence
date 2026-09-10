USE rest ;

CREATE DATABASE rest.staging ;


CREATE TABLE IF NOT EXISTS staging.stg_meteofrance_observation (
    latitude DOUBLE,
    longitude DOUBLE,
    stationId STRING,
    stationName STRING,
    productionTime TIMESTAMP,
    ingestionTime TIMESTAMP ,
    observationTime TIMESTAMP,

    airTemperature DOUBLE,
    dewPointTemperature DOUBLE,
    maxAirTemperature DOUBLE,
    minAirTemperature DOUBLE,

    relativeHumidity DOUBLE,
    maxRelativeHumidity DOUBLE,
    minRelativeHumidity DOUBLE,

    windDirection DOUBLE,
    windSpeed DOUBLE,
    maxWindDirection DOUBLE,
    maxWindSpeed DOUBLE,
    gustDirection DOUBLE,
    maxWindGust DOUBLE,

    hourlyPrecipitation DOUBLE,
    snowDepth DOUBLE,

    soilTemperature10cm DOUBLE,
    soilTemperature20cm DOUBLE,
    soilTemperature50cm DOUBLE,
    soilTemperature100cm DOUBLE,
    groundStateCode INT,


    horizontalVisibility DOUBLE,
    totalCloudCover DOUBLE,
    sunshineDuration DOUBLE,
    hourlyGlobalRadiation DOUBLE,

    stationPressure DOUBLE,
    seaLevelPressure DOUBLE
    )
    USING iceberg ;


CREATE OR REPLACE TABLE staging.stg_meteofrance_paquet (
    meteofrancePrevinum STRING,
    meteofranceModel STRING,
    meteofranceGrid STRING,
    meteofrancePackage STRING,
    meteofranceReferenceTime TIMESTAMP,
    meteofranceTime STRING,
    validTime TIMESTAMP,
    startTime TIMESTAMP,
    endTime TIMESTAMP,
    levelType STRING,
    levelValue DOUBLE,
    isLatLon BOOLEAN,
    projection STRING,
    gridX INT,
    gridY INT,
    latitude DOUBLE,
    longitude DOUBLE,
    variable STRING,
    unit STRING,
    value DOUBLE,
    model STRING,
    grid DOUBLE
)
    USING ICEBERG;