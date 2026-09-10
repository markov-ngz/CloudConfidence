package ngz.cloudconfidence.convertgrib2.parquetconverter;

import ngz.cloudconfidence.convertgrib2.grib2.GridItem;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

public class GridRecordFactory {

    public static final Schema SCHEMA =
            SchemaBuilder.record("GridRecord")
                    .namespace("ngz.cloudconfidence.elt.ingestion")
                    .fields()
                    .requiredString("meteofrance_previnum")
                    .requiredString("meteofrance_model")
                    .requiredString("meteofrance_grid")
                    .requiredString("meteofrance_package")
                    .requiredString("meteofrance_reference_time")
                    .requiredString("meteofrance_time")
                    // Time dimensions
                    .requiredString("validTime")
                    .requiredString("startTime")
                    .requiredString("endTime")
                    // Z dimension
                    .nullableString(
                            "levelType", null) // Made nullable to match prior Spark definition
                    .requiredDouble("levelValue")
                    // 2D dimensions
                    .requiredBoolean("isLatLon")
                    .nullableString("projection", null)
                    .requiredInt("gridX")
                    .requiredInt("gridY")
                    .nullableDouble("lat", -1.0)
                    .nullableDouble("lon", -1.0)
                    // Fact: metric
                    .requiredString("variable")
                    .nullableString("unit", null) // Made nullable to match prior Spark definition
                    .nullableDouble("value", -1.0) // Made nullable to match prior Spark definition
                    .endRecord();

    private final String previnum;
    private final String model;
    private final String grid;
    private final String packageName;
    private final String referenceTime;
    private final String time;

    // Private constructor: use the builder to instantiate
    private GridRecordFactory(Builder builder) {
        this.previnum = builder.previnum;
        this.model = builder.model;
        this.grid = builder.grid;
        this.packageName = builder.packageName;
        this.referenceTime = builder.referenceTime;
        this.time = builder.time;
    }

    /** Maps a transient stream item alongside static file-level metadata into an Avro record. */
    public GenericRecord fromGridItem(GridItem item) {
        GenericRecord record = new GenericData.Record(SCHEMA);

        // Inject shared operational context
        record.put("meteofrance_previnum", previnum);
        record.put("meteofrance_model", model);
        record.put("meteofrance_grid", grid);
        record.put("meteofrance_package", packageName);
        record.put("meteofrance_reference_time", referenceTime);
        record.put("meteofrance_time", time);

        // Map item specific data
        record.put("validTime", item.getValidTime());
        record.put("startTime", item.getStartTime());
        record.put("endTime", item.getEndTime());
        record.put("variable", item.getVariable());
        record.put("unit", item.getUnit());
        record.put("value", item.getValue());
        record.put("levelType", item.getLevelType());
        record.put("levelValue", item.getLevelValue());
        record.put("gridX", item.getGridX());
        record.put("gridY", item.getGridY());
        record.put("isLatLon", item.isLatLon());
        record.put("projection", item.getProjection());
        record.put("lat", item.getLat());
        record.put("lon", item.getLon());

        return record;
    }

    /** Builder to cleanly initialize a fixed metadata template for the parsing scope. */
    public static class Builder {
        private String previnum;
        private String model;
        private String grid;
        private String packageName;
        private String referenceTime;
        private String time;

        public Builder previnum(String previnum) {
            this.previnum = previnum;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder grid(String grid) {
            this.grid = grid;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder referenceTime(String referenceTime) {
            this.referenceTime = referenceTime;
            return this;
        }

        public Builder time(String time) {
            this.time = time;
            return this;
        }

        public GridRecordFactory build() {
            return new GridRecordFactory(this);
        }
    }
}
