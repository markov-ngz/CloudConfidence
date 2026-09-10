package ngz.extraction.core.source;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation: points from a FileSource implementation back to its factory, keeping the
 * caller ignorant of factory details.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FileSourceProvider {
    Class<? extends FileSourceFactory> value();
}
