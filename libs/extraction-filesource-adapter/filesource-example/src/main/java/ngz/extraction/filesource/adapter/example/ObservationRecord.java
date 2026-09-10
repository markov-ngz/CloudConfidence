package ngz.extraction.filesource.adapter.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ObservationRecord {

    private double lat;
    private double lon;

    @JsonProperty("geo_id_insee")
    private int geoIdInsee;

    @JsonProperty("reference_time")
    private Instant referenceTime;

    @JsonProperty("insert_time")
    private Instant insertTime;

    @JsonProperty("validity_time")
    private Instant validityTime;

    private Double t;
    private Double td;
    private Integer u;

    private Integer dd;
    private Double ff;
    private Integer dxi;
    private Double fxi;

    private Double rr1;

    @JsonProperty("t_10")
    private Double t10;

    @JsonProperty("t_20")
    private Double t20;

    @JsonProperty("t_50")
    private Double t50;

    @JsonProperty("t_100")
    private Double t100;

    private Integer vv;

    @JsonProperty("etat_sol")
    private Integer etatSol;

    private Integer sss;

    private Integer insolh;

    @JsonProperty("ray_glo01")
    private Integer rayGlo01;

    private Integer pres;
    private Integer pmer;
}
