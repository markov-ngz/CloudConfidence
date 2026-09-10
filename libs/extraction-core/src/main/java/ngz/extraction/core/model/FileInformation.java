package ngz.extraction.core.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/** File existing on a source. */
@Data
@Builder
public class FileInformation {

    String id; // unique identifier of the specific file
    String location; // location differ from id as it can be common to multiple filesource over
    // time but the id is unique
    String format;

    @Builder.Default private Map<String, String> metadata = new HashMap<String, String>();

    public void setMetadata(String key, String value) {
        this.metadata.put(key, value);
    }
}
