package ngz.extraction.filesink.adapter.azureblob;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import java.util.Map;

public class TokenCredentialFactory {

    public static TokenCredential create(Map<String, String> props) {
        String type = props.getOrDefault("credential.type", "DefaultCredential");

        return switch (type) {
            case "DefaultCredential" -> new DefaultAzureCredentialBuilder().build();

            case "ClientSecretCredential" ->
                    new ClientSecretCredentialBuilder()
                            .tenantId(required(props, "tenantid"))
                            .clientId(required(props, "clientid"))
                            .clientSecret(required(props, "clientsecret"))
                            .build();

            default -> throw new IllegalArgumentException("Unsupported credential type: " + type);
        };
    }

    private static String required(Map<String, String> props, String key) {
        String value = props.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return value;
    }
}
