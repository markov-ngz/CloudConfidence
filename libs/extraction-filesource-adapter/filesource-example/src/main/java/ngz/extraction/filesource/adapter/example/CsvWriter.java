package ngz.extraction.filesource.adapter.example;

import java.util.List;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

/**
 * Converts a list of beans to CSV using Jackson's CSV module. Schema (headers + column order) is
 * derived automatically from the bean's properties.
 */
public final class CsvWriter<T> {

    private final CsvMapper csvMapper;
    private final CsvSchema schema;

    public CsvWriter(Class<T> type, String delimiter) {
        if (delimiter == null || delimiter.length() != 1) {
            throw new IllegalArgumentException(
                    "delimiter must be a single character for Jackson CSV: " + delimiter);
        }

        this.csvMapper = new CsvMapper();
        this.schema =
                csvMapper.schemaFor(type).withColumnSeparator(delimiter.charAt(0)).withHeader();
    }

    public byte[] toCsvBytes(List<T> records) {
        return csvMapper.writer(schema).writeValueAsBytes(records);
    }

    public String toCsv(List<T> records) {
        return csvMapper.writer(schema).writeValueAsString(records);
    }
}
