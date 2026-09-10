package ngz.extraction.core.sink;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SunkFile {
    String filename;
    String uri;
    String format;

    @Builder.Default private Map<String, String> metadata = new HashMap<String, String>();

    public void setMetadata(String key, String value) {
        this.metadata.put(key, value);
    }
}
