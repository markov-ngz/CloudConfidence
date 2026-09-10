package ngz.cloudconfidence.grib2audit;

import java.io.IOException;
import ucar.nc2.dt.grid.GridDataset;

public class GribFileReader {
    public static GridDataset readDataset(String path) {
        try {
            return GridDataset.open(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to audit file at path :" + path, e);
        }
    }
}
