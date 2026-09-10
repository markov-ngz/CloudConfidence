package ngz.cloudconfidence.elt.extraction;

import ngz.cloudconfidence.elt.extraction.config.Configuration;
import ngz.cloudconfidence.elt.extraction.config.ConfigurationLoader;
import ngz.cloudconfidence.elt.extraction.exception.FileExtractionException;
import ngz.etl.filesource.adapter.meteofrance.paquet.MeteoFrancePaquetFileSourceFactory;
import ngz.extraction.core.filename.FileNameGenerator;
import ngz.extraction.core.filename.SimpleFileNameGenerator;
import ngz.extraction.core.sink.FileSink;
import ngz.extraction.core.sink.FileSinkFactory;
import ngz.extraction.core.source.FileSource;
import ngz.extraction.core.source.FileSourceFactory;
import ngz.extraction.filesink.adapter.awsbucket.AwsBucketFileSinkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractData {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractData.class);

    public static void main(String[] args) {
        try {

            // 1. Bootstrapping / Config loading
            Configuration config = ConfigurationLoader.load(args);

            FileSourceFactory sourceFactory = new MeteoFrancePaquetFileSourceFactory();
            FileSinkFactory sinkFactory = new AwsBucketFileSinkFactory();

            FileNameGenerator fileNameGenerator = new SimpleFileNameGenerator(config.getJobId());
            FileSource fileSource = sourceFactory.create(config.getSourceProperties());
            FileSink fileSink = sinkFactory.create(config.getSinkProperties());

            // 2. Instantiate the engine and execute
            FileExtractionEngine engine =
                    new FileExtractionEngine(fileSource, fileSink, fileNameGenerator);
            ExtractionResult result = engine.execute();

            LOG.info(
                    "Extraction completed successfully. Processed {} files.",
                    result.getSuccesses().size());

            System.exit(0);

        } catch (FileExtractionException e) {

            LOG.error("Extraction completed with partial or total failures.", e);
            System.exit(1);
        } catch (Exception e) {
            LOG.error("Critical application failure during initialization.", e);

            System.exit(2);
        }
    }
}
