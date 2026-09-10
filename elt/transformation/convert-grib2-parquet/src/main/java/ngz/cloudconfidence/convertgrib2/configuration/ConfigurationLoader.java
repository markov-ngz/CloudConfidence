package ngz.cloudconfidence.convertgrib2.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Defines how to load the configuration. */
public class ConfigurationLoader {

    private static final String argsPrefix = "--";
    private static final String argsDelimiter = "=";

    private static final String envPrefix = "ETL";

    public static Configuration load(String[] args) {
        Map<String, String> properties = loadProperties(args);

        return Configuration.load(properties);
    }

    private static Map<String, String> loadProperties(String[] args) {
        // 1. Parse sources independently
        Map<String, String> argConfig =
                ConfigurationUtils.parseArgs(args, argsPrefix, argsDelimiter);

        Map<String, String> envConfig = ConfigurationUtils.parseEnv(envPrefix, System.getenv());
        // 4. Merge with correct precedence (low → high)
        Map<String, String> merged = new HashMap<>();

        merged.putAll(envConfig);

        merged.putAll(argConfig);

        return Collections.unmodifiableMap(merged);
    }
}
