package ngz.cloudconfidence.grib2audit;

import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;

/** Grid Structure Audit. */
public class GribStructureAudit {

    public static String audit(GridDataset gridDataset) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n╔══════════════════════════════════════════════════╗\n");
        sb.append("║           GRIB2 STRUCTURE AUDIT                  ║\n");
        sb.append("╚══════════════════════════════════════════════════╝\n");

        for (GridDatatype grid : gridDataset.getGrids()) {
            GridCoordSystem cs = grid.getCoordinateSystem();

            // ── Axes ──────────────────────────────────────────
            CoordinateAxis1DTime timeAxis = cs.getTimeAxis1D();
            CoordinateAxis1D zAxis = cs.getVerticalAxis();
            CoordinateAxis xAxis = cs.getXHorizAxis();
            CoordinateAxis yAxis = cs.getYHorizAxis();

            int nT = timeAxis != null ? (int) timeAxis.getSize() : 1;
            int nZ = zAxis != null ? (int) zAxis.getSize() : 1;
            int nY = (int) yAxis.getSize();
            int nX = (int) xAxis.getSize();

            long totalRows = (long) nT * nZ * nY * nX;

            sb.append(String.format("\n▶ VARIABLE : %s\n", grid.getFullName()));
            sb.append(String.format("  unit        : %s\n", grid.getUnitsString()));
            sb.append(String.format("  description : %s\n", grid.getDescription()));
            sb.append(
                    String.format(
                            "  dims        : time(%d) x z(%d) x y(%d) x x(%d) = %,d grid points\n",
                            nT, nZ, nY, nX, totalRows));

            // ── Time samples ──────────────────────────────────
            if (timeAxis != null && nT > 0) {
                CalendarDate first = timeAxis.getCalendarDateRange().getStart();
                CalendarDate last = timeAxis.getCalendarDateRange().getEnd();
                sb.append(String.format("  time range  : %s → %s\n", first, last));
            }

            // ── Vertical levels ───────────────────────────────
            if (zAxis != null && nZ > 0) {
                sb.append(
                        String.format(
                                "  z-axis type : %s | unit: %s\n",
                                zAxis.getAxisType(), zAxis.getUnitsString()));
                sb.append("  z values    : [");
                for (int i = 0; i < Math.min(nZ, 6); i++) {
                    sb.append(String.format("%.1f ", zAxis.getCoordValue(i)));
                }
                sb.append(nZ > 6 ? "... ]\n" : "]\n");
            }

            // ── Grid bounds (lat/lon corners) ─────────────────
            ucar.unidata.geoloc.LatLonRect bbox = cs.getLatLonBoundingBox();
            sb.append(
                    String.format(
                            "  bbox        : lat[%.3f → %.3f] lon[%.3f → %.3f]\n",
                            bbox.getLatMin(),
                            bbox.getLatMax(),
                            bbox.getLonMin(),
                            bbox.getLonMax()));

            // ── Data type & fill value ─────────────────────────
            sb.append(String.format("  java type   : %s\n", grid.getVariable().getDataType()));
            Object fill = grid.getVariable().findAttribute("missing_value");
            sb.append(String.format("  fill value  : %s\n", fill));

            // ── Sample data stats (first time/level slice) ────
            try {
                ucar.ma2.Array slice = grid.readDataSlice(0, 0, -1, -1);
                ucar.ma2.MAMath.MinMax mm = ucar.ma2.MAMath.getMinMax(slice);
                long nanCount = countNaN(slice);
                sb.append(String.format("  data range  : [%.4f → %.4f]\n", mm.min, mm.max));
                sb.append(
                        String.format(
                                "  NaN count   : %,d / %,d (%.1f%%)\n",
                                nanCount, (long) nY * nX, 100.0 * nanCount / ((long) nY * nX)));
            } catch (Exception e) {
                sb.append(String.format("  [could not read slice: %s]\n", e.getMessage()));
            }
        }

        return sb.toString();
    }

    private static long countNaN(ucar.ma2.Array arr) {
        long count = 0;
        var iter = arr.getIndexIterator();
        while (iter.hasNext()) {
            double v = iter.getDoubleNext();
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                count++;
            }
        }
        return count;
    }
}
