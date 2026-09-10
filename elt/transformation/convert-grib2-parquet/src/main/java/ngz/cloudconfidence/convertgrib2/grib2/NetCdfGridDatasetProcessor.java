package ngz.cloudconfidence.convertgrib2.grib2;

import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.Data;
import ngz.cloudconfidence.convertgrib2.grib2.exception.DatasetProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;

public class NetCdfGridDatasetProcessor implements GridDatasetProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(NetCdfGridDatasetProcessor.class);
    private final TemporaryFileDownloader temporaryFileDownloader;

    public NetCdfGridDatasetProcessor(TemporaryFileDownloader temporaryFileDownloader) {
        this.temporaryFileDownloader = temporaryFileDownloader;
    }

    @Override
    public void process(String filepath, GridRecordConsumer consumer)
            throws DatasetProcessingException {
        // Download remote .grib2 file on temporary local file and open it as gridataset
        try (TemporaryFileDownloader.LocalTempFile tempFile =
                        temporaryFileDownloader.resolveToLocal(filepath);
                GridDataset gridDataset = GridDataset.open(tempFile.getPath())) {

            // Stream all points across all dimensions to be processed by consumer
            for (GridDatatype grid : gridDataset.getGrids()) {
                streamGridPoints(grid, consumer);
            }

        } catch (IOException e) {
            throw new DatasetProcessingException(
                    "Failed to read/access grid dataset at: " + filepath, e);
        } catch (Exception e) {
            throw new DatasetProcessingException(
                    "Error occurred while streaming dataset records", e);
        }
    }

    private void streamGridPoints(GridDatatype grid, GridRecordConsumer consumer) throws Exception {
        // 1. Top level info of GridDatatype
        GridMetadata meta = extractMetadata(grid);
        GridCoordSystem cs = grid.getCoordinateSystem();

        // Loop through time, vertical levels, and spatial coordinates
        for (int t = 0; t < meta.getNT(); t++) {

            for (int z = 0; z < meta.getNZ(); z++) {

                // 2D Array information
                Array slice = grid.readDataSlice(t, z, -1, -1);
                int[] shape = slice.getShape(); // [lon, lat]
                int latSize = shape[1];
                int lonSize = shape[0];
                Index index = slice.getIndex();
                slice.getIndexIterator().next();

                for (int x = 0; x < latSize; x++) {
                    for (int y = 0; y < lonSize; y++) {
                        double value = slice.getDouble(index.set(y, x));
                        GridItem point = buildGridItem(grid, meta, t, z, x, y, value);
                        consumer.accept(point);
                    }
                }
            }
        }
    }

    @Data
    @AllArgsConstructor
    private static class GridMetadata {
        private int nT; // Number of time steps
        private int nZ; // Number of vertical levels
        private String startTime;
        private String endTime;
        private String levelType;
        private String projection;
        private boolean isLatLon;
        private String variable;
        private String unit;
    }

    private GridMetadata extractMetadata(GridDatatype grid) {
        GridCoordSystem cs = grid.getCoordinateSystem();
        CoordinateAxis1DTime timeAxis = cs.getTimeAxis1D();
        CoordinateAxis1D zAxis = cs.getVerticalAxis();

        int nT = timeAxis != null ? (int) timeAxis.getSize() : 1;
        int nZ = zAxis != null ? (int) zAxis.getSize() : 1;

        String startTime =
                timeAxis != null
                        ? timeAxis.getCalendarDateRange().getStart().toString()
                        : "unknown";
        String endTime =
                timeAxis != null ? timeAxis.getCalendarDateRange().getEnd().toString() : "unknown";
        String levelType = zAxis != null ? zAxis.getAxisType().toString() : null;
        String projection = cs.isLatLon() ? null : cs.getProjectionCT().getName();

        return new GridMetadata(
                nT,
                nZ,
                startTime,
                endTime,
                levelType,
                projection,
                cs.isLatLon(),
                grid.getName(),
                grid.getUnitsString());
    }

    private GridItem buildGridItem(
            GridDatatype grid,
            GridMetadata meta,
            int timeIdx,
            int levelIdx,
            int x,
            int y,
            double value) {
        GridCoordSystem cs = grid.getCoordinateSystem();
        CoordinateAxis1DTime timeAxis = cs.getTimeAxis1D();
        CoordinateAxis1D zAxis = cs.getVerticalAxis();

        String validTime = timeAxis != null ? timeAxis.getCalendarDate(timeIdx).toString() : null;
        double levelValue = zAxis != null ? zAxis.getCoordValue(levelIdx) : 0.0;
        LatLonPoint latLon = cs.getLatLon(x, y);

        return new GridItem(
                validTime,
                meta.getStartTime(),
                meta.getEndTime(),
                meta.getVariable(),
                meta.getUnit(),
                value,
                meta.getLevelType(),
                levelValue,
                x,
                y,
                meta.isLatLon(),
                meta.getProjection(),
                latLon != null ? latLon.getLatitude() : null,
                latLon != null ? latLon.getLongitude() : null);
    }
}
