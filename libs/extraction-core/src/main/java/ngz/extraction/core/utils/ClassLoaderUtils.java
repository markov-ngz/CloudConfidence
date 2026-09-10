package ngz.extraction.core.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public class ClassLoaderUtils {

    /** Executes a given action under a specific Context ClassLoader. */
    public static <T> T withClassLoader(ClassLoader targetLoader, Supplier<T> action) {
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(targetLoader);
            return action.get();
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    /**
     * Creates a URLClassLoader for a list of JAR paths using an explicit parent ClassLoader. This
     * is the most generic approach.
     */
    public static URLClassLoader loadJars(List<Path> jarPaths, ClassLoader parentLoader)
            throws IOException {
        URL[] urls = new URL[jarPaths.size()];

        for (int i = 0; i < jarPaths.size(); i++) {
            Path jarPath = jarPaths.get(i);

            if (!Files.exists(jarPath)) {
                throw new FileNotFoundException(
                        "Plugin JAR not found: " + jarPath.toAbsolutePath());
            }
            // toURL() throws MalformedURLException, handled by throwing IOException
            urls[i] = jarPath.toUri().toURL();
        }

        return new URLClassLoader(urls, parentLoader);
    }
}
