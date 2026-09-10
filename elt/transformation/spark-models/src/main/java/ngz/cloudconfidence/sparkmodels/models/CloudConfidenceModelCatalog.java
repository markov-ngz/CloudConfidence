package ngz.cloudconfidence.sparkmodels.models;

import java.util.List;
import ngz.cloudconfidence.sparkmodels.models.intermediate.IntAggForecastErrorOverTimeModel;
import ngz.cloudconfidence.sparkmodels.models.intermediate.IntStationForecastObservationModel;
import ngz.cloudconfidence.sparkmodels.models.intermediate.IntStationGridMappingModel;
import ngz.cloudconfidence.sparkmodels.models.intermediate.IntUnpivotObservationModel;
import ngz.cloudconfidence.sparkmodels.models.mart.MartForecastErrorOverTimeModel;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedMeteoFranceObservationVariable;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedMeteoFranceStation;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedUnit;
import ngz.cloudconfidence.sparkmodels.models.seed.SeedStandardizedVariable;
import ngz.cloudconfidence.sparkmodels.models.source.SourceMeteoFranceObservation;
import ngz.cloudconfidence.sparkmodels.models.source.SourceMeteoFrancePaquet;
import ngz.cloudconfidence.sparkmodels.models.staging.StgMeteoFranceObservationModel;
import ngz.cloudconfidence.sparkmodels.models.staging.StgMeteoFrancePaquetModel;
import ngz.markov.sparkmodel.model.SparkModel;

public class CloudConfidenceModelCatalog {
    public static final List<Class<? extends SparkModel>> MODELZ =
            List.of(
                    // Seeds
                    SeedMeteoFranceObservationVariable.class,
                    SeedMeteoFranceStation.class,
                    SeedStandardizedUnit.class,
                    SeedStandardizedVariable.class,

                    // Source
                    SourceMeteoFrancePaquet.class,
                    SourceMeteoFranceObservation.class,

                    // Staging
                    StgMeteoFranceObservationModel.class,
                    StgMeteoFrancePaquetModel.class,

                    // Intermediate
                    IntStationGridMappingModel.class,
                    IntUnpivotObservationModel.class,
                    IntStationForecastObservationModel.class,
                    IntAggForecastErrorOverTimeModel.class,

                    // Mart
                    MartForecastErrorOverTimeModel.class);
}
