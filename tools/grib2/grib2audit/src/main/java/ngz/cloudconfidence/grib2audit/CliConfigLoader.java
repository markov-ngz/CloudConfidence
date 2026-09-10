package ngz.cloudconfidence.grib2audit;

import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;

public class CliConfigLoader {

    public static Configuration loadConfiguration(String[] args) throws ParseException {
        // Define the CLI options
        Options options = new Options();

        // Required: Input file path
        Option fileOption =
                Option.builder("f")
                        .longOpt("file")
                        .hasArg()
                        .argName("FILE")
                        .desc("Path to the input GRIB2 file (required)")
                        .required()
                        .get();
        options.addOption(fileOption);

        // Optional: Output file path
        Option outputOption =
                Option.builder("o")
                        .longOpt("output")
                        .hasArg()
                        .argName("OUTPUT")
                        .desc("Path to save the audit output")
                        .get();
        options.addOption(outputOption);

        // Optional: Print to console
        Option printOption =
                Option.builder("np")
                        .longOpt("no-print")
                        .desc("Do not Print the audit to console")
                        .get();

        options.addOption(printOption);

        // Optional: Help
        Option helpOption =
                Option.builder("h").longOpt("help").desc("Print this help message").get();
        options.addOption(helpOption);

        // Parse the command line
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // Handle help request
        if (cmd.hasOption("help")) {
            HelpFormatter formatter = HelpFormatter.builder().get();
            try {
                formatter.printHelp("App", "Header", options, "Footer", true);

                System.exit(0);

            } catch (IOException e) {
                throw new RuntimeException("Failed to print help", e);
            }
        }

        // Populate the Configuration object
        return Configuration.builder()
                .filepath(cmd.getOptionValue("file"))
                .outputPath(cmd.getOptionValue("output"))
                .print(!cmd.hasOption("no-print"))
                .build();
    }
}
