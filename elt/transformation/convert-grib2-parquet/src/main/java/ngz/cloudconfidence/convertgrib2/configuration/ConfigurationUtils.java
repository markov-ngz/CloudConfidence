package ngz.cloudconfidence.convertgrib2.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Utility methods for configuration. */
public class ConfigurationUtils {

    public static Map<String, String> subMap(Map<String, String> config, String prefix) {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, String> entry : config.entrySet()) {
            if (entry.getKey().startsWith(prefix + ".")) {
                String newKey = entry.getKey().substring(prefix.length() + 1);
                result.put(newKey, entry.getValue());
            }
        }

        return result;
    }

    public static Map<String, String> parseArgs(
            String[] args, String argPrefix, String argDelimiter) {

        Map<String, String> map = new HashMap<>();

        if (args == null || args.length == 0) {
            return map;
        }

        for (String arg : args) {

            if (arg == null || arg.isEmpty()) {
                continue;
            }

            // Remove prefix if present (e.g. "--")
            if (argPrefix != null && !argPrefix.isEmpty() && arg.startsWith(argPrefix)) {
                arg = arg.substring(argPrefix.length());
            }

            String key;
            String value;

            if (argDelimiter != null) {
                // STRICT mode: only key=value allowed
                if (!arg.contains(argDelimiter)) {
                    // ignore or throw, depending on how strict you want
                    continue;
                    // or: throw new IllegalArgumentException("Invalid arg: " + arg);
                }

                String[] parts = arg.split(argDelimiter, 2);
                key = parts[0];
                value = parts.length > 1 ? parts[1] : "";
            } else {
                // loose / undefined behavior (as you said you're fine with)
                key = arg;
                value = "true";
            }

            map.put(key.toLowerCase(), value);
        }

        return map;
    }

    public static Map<String, String> parseEnv(String prefix, Map<String, String> env) {

        Map<String, String> map = new HashMap<>();

        if (prefix == null) {
            return map;
        }

        String normalizedPrefix = prefix.toUpperCase() + "_";

        for (Map.Entry<String, String> entry : env.entrySet()) {

            String key = entry.getKey().toUpperCase();

            if (key.startsWith(normalizedPrefix)) {

                String normalized = key.toLowerCase().replace("_", ".");

                String strippedValue = stripQuotes(entry.getValue());

                map.put(normalized, strippedValue);
            }
        }

        return map;
    }

    public static String stripQuotes(String value) {

        value = value.strip();

        if (value == null) {
            return null;
        }

        // first chars
        if (!value.isEmpty()) {
            char first = value.charAt(0);
            if (first == '"' || first == '\'') {
                value = value.substring(1);
            }
        }

        // last chars
        if (!value.isEmpty()) {
            char last = value.charAt(value.length() - 1);
            if (last == '"' || last == '\'') {
                value = value.substring(0, value.length() - 1);
            }
        }

        return value;
    }

    private static void flatten(String prefix, Object value, Map<String, String> map) {
        if (value instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                String key = entry.getKey().toString();
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                flatten(newPrefix, entry.getValue(), map);
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                String newPrefix = prefix + "[" + i + "]";
                flatten(newPrefix, list.get(i), map);
            }
        } else if (value != null) {
            map.put(prefix, value.toString());
        }
    }
}
