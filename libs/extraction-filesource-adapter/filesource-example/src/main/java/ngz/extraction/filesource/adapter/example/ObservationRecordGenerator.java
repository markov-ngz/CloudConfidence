package ngz.extraction.filesource.adapter.example;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.datafaker.Faker;

public class ObservationRecordGenerator implements DataGenerator<ObservationRecord> {

    private final Faker faker;

    public ObservationRecordGenerator() {
        this.faker = new Faker();
    }

    @Override
    public Class<ObservationRecord> getType() {
        return ObservationRecord.class;
    }

    @Override
    public ObservationRecord generate() {

        Instant validityTime = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Instant insertTime = validityTime.plusSeconds(faker.number().numberBetween(30, 300));
        Instant referenceTime = insertTime.plusSeconds(faker.number().numberBetween(30, 300));

        return ObservationRecord.builder()
                .lat(faker.number().randomDouble(6, 41, 51))
                .lon(faker.number().randomDouble(6, -5, 10))
                .geoIdInsee(faker.number().numberBetween(10000000, 99999999))
                .validityTime(validityTime)
                .insertTime(insertTime)
                .referenceTime(referenceTime)
                // Air temperature (Kelvin)
                .t(faker.number().randomDouble(2, 260, 320))
                .td(faker.number().randomDouble(2, 250, 315))
                // Relative humidity (%)
                .u(faker.number().numberBetween(0, 101))
                // Wind direction (degrees)
                .dd(faker.number().numberBetween(0, 361))
                // Wind speed (m/s)
                .ff(faker.number().randomDouble(1, 0, 30))
                // Max wind direction over 10 min
                .dxi(faker.number().numberBetween(0, 361))
                // Max wind speed over 10 min
                .fxi(faker.number().randomDouble(1, 0, 50))
                // Rainfall
                .rr1(faker.number().randomDouble(1, 0, 20))
                // Soil temperatures (Kelvin)
                .t10(faker.number().randomDouble(2, 260, 320))
                .t20(faker.number().randomDouble(2, 260, 320))
                .t50(faker.number().randomDouble(2, 260, 320))
                .t100(faker.number().randomDouble(2, 260, 320))
                // Visibility (meters)
                .vv(faker.number().numberBetween(0, 50000))
                // Ground state code
                .etatSol(faker.number().numberBetween(0, 10))
                // Sunshine duration
                .sss(faker.number().numberBetween(0, 60))
                // Sunshine hour indicator
                .insolh(faker.number().numberBetween(0, 24))
                // Global radiation
                .rayGlo01(faker.number().numberBetween(0, 1500000))
                // Pressure (Pa)
                .pres(faker.number().numberBetween(95000, 105000))
                // Sea-level pressure (Pa)
                .pmer(faker.number().numberBetween(95000, 105000))
                .build();
    }
}
