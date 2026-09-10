package ngz.extraction.filesource.adapter.example;

import ngz.extraction.core.file.FileInformation;
import ngz.extraction.core.file.source.FileSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ExampleFileSource implements FileSource {

    private final DataGenerator dataGenerator;

    private final int fileInfoNumber;

    private final CsvWriter csvWriter;

    public ExampleFileSource(DataGenerator dataGenerator, int fileInfoNumber, String csvDelimiter) {

        this.dataGenerator = dataGenerator;
        this.fileInfoNumber = fileInfoNumber;
        this.csvWriter = new CsvWriter(dataGenerator.getType(), csvDelimiter);
    }

    @Override
    public List<FileInformation> listFiles() {
        List<FileInformation> fileInformations = new ArrayList<>();

        int i = 0;

        Map<String, String> metadata = new HashMap<>();

        metadata.put("value", "example");

        while (i < 3) {
            String location = "Location-" + String.valueOf(i);

            metadata.put("file", location);

            FileInformation fileInformation =
                    FileInformation.builder()
                            .id(String.valueOf(i))
                            .location(location)
                            .format("csv")
                            .metadata(metadata)
                            .build();
            fileInformations.add(fileInformation);
            i++;
        }

        return fileInformations;
    }

    @Override
    public byte[] downloadFile(String fileLocation) {
        List<Object> records = new ArrayList<>();

        int i = 0;
        int recordNumber = 100;
        while (i < recordNumber) {
            records.add(this.dataGenerator.generate());
            i++;
        }

        return csvWriter.toCsvBytes(records);
    }
}
