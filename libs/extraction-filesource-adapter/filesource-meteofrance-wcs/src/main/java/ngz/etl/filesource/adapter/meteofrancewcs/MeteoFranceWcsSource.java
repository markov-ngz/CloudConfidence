package ngz.etl.filesource.adapter.meteofrancewcs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import ngz.etl.core.file.FileInformation;
import ngz.etl.core.file.FileSource;
import ngz.etl.core.file.FileSourceProvider;
import ngz.etl.core.file.exception.FileSourceException;
import ngz.meteofrance.wcs.factory.MeteoFranceCoverageFileFactory;
import ngz.meteofrance.wcs.model.MeteoFranceCoverageFile;
import ngz.meteofrance.wcs.service.MeteoFranceWcsService;
import ngz.meteofrance.wcs.tools.CoverageSummaryExperimental;
import ngz.opengis.wcs.exception.WcsException;
import ngz.opengis.wcs.model.Capabilities;
import ngz.opengis.wcs.model.Coverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Adapter of the WcsService for FileSource. */
@FileSourceProvider(MeteoFranceWcsSourceFactory.class)
public class MeteoFranceWcsSource implements FileSource {

    private final MeteoFranceWcsService wcsService;
    private final List<String> requestedCoverageTitles;
    private final Instant startTime;
    private final Instant endTime;
    private final transient MeteoFranceCoverageFileFactory factory =
            new MeteoFranceCoverageFileFactory();
    private static final Logger LOG = LoggerFactory.getLogger(MeteoFranceWcsSource.class);

    public MeteoFranceWcsSource(
            MeteoFranceWcsService wcsService,
            List<String> requestedCoverageTitles,
            Instant startTime,
            Instant endTime) {
        this.wcsService = wcsService;
        this.requestedCoverageTitles = requestedCoverageTitles;
        this.startTime = startTime;
        this.endTime = endTime;

        LOG.info(requestedCoverageTitles.toString());
    }

    @Override
    public List<FileInformation> listFiles() throws FileSourceException {

        LOG.info("Starting to list files from MeteoFrance WCS source");

        Capabilities capabilities;
        try {
            LOG.info("Fetching capabilities from WCS service");
            capabilities = wcsService.getCapabilities();
            LOG.info(
                    "Successfully retrieved capabilities with {} coverage summaries",
                    capabilities.getSummaries().size());
        } catch (WcsException e) {
            LOG.error("Failed to get capabilities from WCS service: {}", e.getMessage());
            throw new FileSourceException(
                    "Failed to get capabilities from WCS service: " + e.getMessage(), e);
        }

        List<FileInformation> fileInformations = new ArrayList<>();
        List<String> failedCoverages = new ArrayList<>();

        LOG.info("Processing {} coverage summaries", capabilities.getSummaries().size());

        List<String> uniqueTitles =
                capabilities.getSummaries().stream()
                        .map(s -> s.getTitle() + "|")
                        .distinct()
                        .toList();
        LOG.info("Unique titles founds {}", uniqueTitles);

        for (Capabilities.CoverageSummary coverageSummary : capabilities.getSummaries()) {
            try {
                LOG.info("Processing coverage summary: {}", coverageSummary.getId());
                // Infer referenceTime
                Instant referenceTime =
                        CoverageSummaryExperimental.inferReferenceTimeFromCoverageId(
                                coverageSummary.getId());

                if (skipCoverageSummary(referenceTime, coverageSummary.getTitle())) {
                    LOG.debug(
                            "Skipping coverage {} as it doesn't match criteria",
                            coverageSummary.getId());
                    continue;
                }

                LOG.info(
                        "Processing coverage {} - title: {}",
                        coverageSummary.getId(),
                        coverageSummary.getTitle());
                List<FileInformation> inloopFileInformations =
                        processCoverageSummary(
                                coverageSummary, capabilities.getGetCoverageUrl(), referenceTime);

                fileInformations.addAll(inloopFileInformations);
                LOG.info(
                        "Successfully processed coverage {} - found {} files",
                        coverageSummary.getId(),
                        inloopFileInformations.size());

            } catch (FileSourceException e) {
                // Log the error but continue processing other coverages
                LOG.error(
                        "Error processing coverage {}: {}",
                        coverageSummary.getId(),
                        e.getMessage());
                failedCoverages.add(coverageSummary.getId());
            }
        }

        if (!failedCoverages.isEmpty()) {
            LOG.error(
                    "Failed to process {} coverages: {}",
                    failedCoverages.size(),
                    String.join(", ", failedCoverages));
            throw new FileSourceException(
                    "Failed to process "
                            + failedCoverages.size()
                            + " coverages: "
                            + String.join(", ", failedCoverages));
        }

        LOG.info(
                "Successfully processed all coverages - found {} total files",
                fileInformations.size());
        return fileInformations;
    }

    private boolean skipCoverageSummary(Instant referenceTime, String coverageSummaryTitle) {
        LOG.debug(
                "Checking if coverage '{}' should be skipped (referenceTime: {}, startTime: {}, endTime: {})",
                coverageSummaryTitle,
                referenceTime,
                startTime,
                endTime);

        // 1. Filter out titles we don't care about
        if (!requestedCoverageTitles.contains(coverageSummaryTitle)) {
            LOG.debug("Skipping coverage '{}' - not in requested titles", coverageSummaryTitle);
            return true;
        } else if (referenceTime.isBefore(startTime) || referenceTime.isAfter(endTime)) {
            LOG.debug("Skipping coverage '{}' - outside time range", coverageSummaryTitle);
            return true;
        }

        LOG.debug("Including coverage '{}' - matches all criteria", coverageSummaryTitle);
        return false;
    }

    private List<FileInformation> processCoverageSummary(
            Capabilities.CoverageSummary coverageSummary,
            String capabilitiesGetDescribeCoverageUrl,
            Instant referenceTime)
            throws FileSourceException {

        try {
            Coverage coverage = wcsService.describeCoverage(coverageSummary.getId());

            List<MeteoFranceCoverageFile> meteoFranceFiles =
                    factory.createFiles(
                            coverage,
                            capabilitiesGetDescribeCoverageUrl,
                            this.wcsService.getModel(),
                            this.wcsService.getModelType().name(),
                            referenceTime,
                            coverageSummary.getTitle());

            // Convert MeteoFranceCoverageFile to FileInformation
            List<FileInformation> fileInformations = new ArrayList<>();
            for (MeteoFranceCoverageFile file : meteoFranceFiles) {
                fileInformations.add(MeteoFranceCoverageFileMapper.mapToFileInformation(file));
            }
            return fileInformations;

        } catch (WcsException e) {
            throw new FileSourceException(
                    "Failed to process coverage summary for "
                            + coverageSummary.getId()
                            + ": "
                            + e.getMessage(),
                    e);
        }
    }

    @Override
    public byte[] downloadFile(String fileLocation) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
