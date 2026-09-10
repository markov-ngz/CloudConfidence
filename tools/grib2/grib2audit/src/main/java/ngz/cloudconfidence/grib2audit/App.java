package ngz.cloudconfidence.grib2audit;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.cli.ParseException;
import ucar.nc2.dt.grid.GridDataset;

public class App {
    public static void main(String[] args) {
        try {
            // 1. Load configuration from CLI
            Configuration config = CliConfigLoader.loadConfiguration(args);

            // 2. Open the file
            GridDataset gridDataset = GribFileReader.readDataset(config.filepath);

            // 3. Audit
            String audit = GribStructureAudit.audit(gridDataset);

            // 4. Write or print
            if (config.outputPath != null) {
                Files.writeString(Paths.get(config.outputPath), audit, StandardCharsets.UTF_8);
            }
            if (config.print) {
                System.out.print(audit);
            }
        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
