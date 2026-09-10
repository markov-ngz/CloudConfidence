package ngz.cloudconfidence.grib2audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class Configuration {
    String filepath; // local file path to read the file
    String outputPath; // optional : output path of the audit structure
    @Builder.Default boolean print = true; // optional : print the audit or not
}
