package ngz.cloudconfidence.elt.extraction;

import lombok.Getter;
import ngz.extraction.core.sink.SunkFile;

@Getter
public class ProcessResult {
    private final SunkFile sunkFile;
    private final Exception exception;

    private ProcessResult(SunkFile sunkFile, Exception exception) {
        this.sunkFile = sunkFile;
        this.exception = exception;
    }

    public static ProcessResult success(SunkFile sunkFile) {
        return new ProcessResult(sunkFile, null);
    }

    public static ProcessResult failure(Exception e) {
        return new ProcessResult(null, e);
    }

    public boolean isSuccess() {
        return exception == null;
    }
}
