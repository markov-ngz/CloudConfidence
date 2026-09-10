package ngz.cloudconfidence.sparkmodels.application.persistence;

import java.util.Objects;

/**
 * Holds the active {@link TableWriter} implementation.
 *
 * <p>Initialized once at application startup (Spark entry point, DI config, etc). After that, all
 * static facades delegate here without needing injection at every call site.
 *
 * <p>This is the single place where the infrastructure choice is made.
 */
public final class TableWriterRegistry {

    private static volatile TableWriter instance;

    private TableWriterRegistry() {}

    /**
     * Register the implementation to use. Must be called once before any write operation —
     * typically in your Spark job's main().
     */
    public static void register(TableWriter writer) {
        Objects.requireNonNull(writer, "TableWriter implementation must not be null");
        instance = writer;
    }

    /**
     * Returns the registered implementation.
     *
     * @throws IllegalStateException if no implementation has been registered yet.
     */
    public static TableWriter get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "No TableWriter registered. Call TableWriterRegistry.register() at startup.");
        }
        return instance;
    }

    /** Resets the registry — intended for tests only. */
    static void reset() {
        instance = null;
    }
}
