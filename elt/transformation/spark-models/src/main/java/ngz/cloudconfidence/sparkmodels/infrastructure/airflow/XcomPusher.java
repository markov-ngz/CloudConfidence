package ngz.cloudconfidence.sparkmodels.infrastructure.airflow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** Airflow Xcom utility class. */
public class XcomPusher {

    // Default Airflow XCom path
    private static final String DEFAULT_XCOM_PATH = "/airflow/xcom/return.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    /** Pushes data to the default Airflow XCom path. */
    public static void pushXcom(Object data, boolean success) {
        pushXcom(data, success, DEFAULT_XCOM_PATH);
    }

    /**
     * Pushes data to a specified XCom file path. * @param data The payload to send to Airflow
     *
     * @param success Boolean status to indicate task state
     * @param filePath Custom path to write the JSON file
     */
    public static void pushXcom(Object data, boolean success, String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            filePath = DEFAULT_XCOM_PATH;
        }

        // Create the JSON structure
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("status", success ? "success" : "failed");

        // Add classType
        rootNode.put("object", data.getClass().getName());
        // Convert the data object to a JSON tree and add it
        rootNode.set("data", mapper.valueToTree(data));

        // Ensure parent directories exist
        Path path = Paths.get(filePath);
        Path parent = path.getParent();

        try {
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            // Write to disk
            mapper.writeValue(path.toFile(), rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write XCom to path: " + filePath, e);
        }

        System.out.println("Successfully wrote XCom to: " + filePath);
        System.out.println("Payload: " + rootNode.toString());
    }
}
