package ngz.extraction.core.source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import ngz.extraction.core.utils.ClassLoaderUtils;

public class FileSourceClassLoader {

    public FileSource createFileSource(
            List<Path> jarPaths, String pluginClassName, Map<String, String> props) {

        try {
            // 1. Create the isolated classloader for these JARs
            // We pass the current class's classloader as the parent
            ClassLoader jarClassLoader =
                    ClassLoaderUtils.loadJars(
                            jarPaths, FileSourceClassLoader.class.getClassLoader());

            // 2. Run the allocation inside the context of that isolated classloader
            return ClassLoaderUtils.withClassLoader(
                    jarClassLoader,
                    () -> {
                        try {
                            // Load the class safely inside the thread context
                            Class<?> rawClass = jarClassLoader.loadClass(pluginClassName);

                            // Assert it implements/extends what you expect (FileSource, DbSource,
                            // etc.)
                            if (!FileSource.class.isAssignableFrom(rawClass)) {
                                throw new IllegalArgumentException(
                                        pluginClassName
                                                + " is not a valid "
                                                + FileSource.class.getSimpleName());
                            }

                            // Safely cast and instantiate
                            Class<? extends FileSource> targetClass =
                                    rawClass.asSubclass(FileSource.class);
                            FileSourceProvider provider =
                                    targetClass.getAnnotation(FileSourceProvider.class);

                            FileSourceFactory factory =
                                    provider.value().getDeclaredConstructor().newInstance();

                            return factory.create(props);

                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException("Failed to load plugin from context", e);
                        }
                    });

        } catch (IOException e) {
            throw new RuntimeException("Failed to read plugin JAR files", e);
        }
    }
}
