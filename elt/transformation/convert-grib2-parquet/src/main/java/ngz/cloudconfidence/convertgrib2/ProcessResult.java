package ngz.cloudconfidence.convertgrib2;

import lombok.Getter;

@Getter
public class ProcessResult {

    private final String uri;
    private final String outputFile;
    private final boolean success;
    private final String errorMessage;

    public ProcessResult(String uri, String outputFile, boolean success, String errorMessage) {
        this.uri = uri;
        this.outputFile = outputFile;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static ProcessResult success(String uri, String outputFile) {
        return new ProcessResult(uri, outputFile, true, null);
    }

    public static ProcessResult failure(String uri, String message) {
        return new ProcessResult(uri, null, false, message);
    }
}
