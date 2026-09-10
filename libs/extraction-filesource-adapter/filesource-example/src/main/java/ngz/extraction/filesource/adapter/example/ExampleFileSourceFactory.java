package ngz.extraction.filesource.adapter.example;

import java.util.Map;
import ngz.extraction.core.file.source.FileSource;
import ngz.extraction.core.file.source.FileSourceFactory;

public class ExampleFileSourceFactory implements FileSourceFactory {

    private final int defaultRecordNumber = 100;
    private final String defaultCsvDelimiter = ";";

    @Override
    public FileSource create(Map<String, String> props) {

        DataGenerator dataGenerator = new ObservationRecordGenerator();

        return new ExampleFileSource(dataGenerator, defaultRecordNumber, defaultCsvDelimiter);
    }
}
