package ngz.cloudconfidence.convertgrib2.grib2;

@FunctionalInterface
public interface GridRecordConsumer {
    void accept(GridItem record) throws Exception;
}
