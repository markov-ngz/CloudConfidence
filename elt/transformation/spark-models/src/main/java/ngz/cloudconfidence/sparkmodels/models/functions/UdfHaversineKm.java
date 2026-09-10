package ngz.cloudconfidence.sparkmodels.models.functions;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;

public class UdfHaversineKm {
    public static void register(SparkSession spark) {
        spark.udf()
                .register(
                        "haversineKm",
                        (Double lat1, Double lon1, Double lat2, Double lon2) -> {
                            if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
                                return null;
                            }
                            final double r = 6371.0;
                            double dLat = Math.toRadians(lat2 - lat1);
                            double dLon = Math.toRadians(lon2 - lon1);
                            double a =
                                    Math.sin(dLat / 2) * Math.sin(dLat / 2)
                                            + Math.cos(Math.toRadians(lat1))
                                                    * Math.cos(Math.toRadians(lat2))
                                                    * Math.sin(dLon / 2)
                                                    * Math.sin(dLon / 2);
                            return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                        },
                        DataTypes.DoubleType);
    }
}
