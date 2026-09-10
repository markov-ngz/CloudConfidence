package ngz.cloudconfidence.convertgrib2.grib2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemporaryFileDownloader {

    private static final Logger LOG = LoggerFactory.getLogger(TemporaryFileDownloader.class);
    private final Configuration configuration;

    public TemporaryFileDownloader(Configuration configuration) {
        this.configuration = configuration;
    }

    public LocalTempFile resolveToLocal(String uri) throws IOException {
        if (uri.startsWith("/") && uri.startsWith("./")) {
            LOG.info("Local file read at : {}", uri);
            return new LocalTempFile(uri, false);
        }

        String localPathStr = "/tmp/" + UUID.randomUUID() + ".grib2";
        LOG.info("Downloading remote file for processing: {}", uri);
        FileSystem fs = FileSystem.get(URI.create(uri), configuration);
        fs.copyToLocalFile(new Path(uri), new Path(localPathStr));

        LOG.info("Successfully saved to: {}", localPathStr);
        return new LocalTempFile(localPathStr, true);
    }

    /** A wrapper that remembers if the file was copied to /tmp and deletes it when closed. */
    public static class LocalTempFile implements AutoCloseable {
        private final String localPath;
        private final boolean needsCleanup;

        public LocalTempFile(String localPath, boolean needsCleanup) {
            this.localPath = localPath;
            this.needsCleanup = needsCleanup;
        }

        public String getPath() {
            return localPath;
        }

        @Override
        public void close() {
            if (needsCleanup) {
                File f = new File(localPath);
                if (f.exists() && f.delete()) {
                    LOG.info("Cleaned up temporary disk space: {}", localPath);
                }
            }
        }
    }
}
