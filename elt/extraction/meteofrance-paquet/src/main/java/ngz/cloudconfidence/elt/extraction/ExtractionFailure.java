package ngz.cloudconfidence.elt.extraction;

import lombok.Getter;
import ngz.extraction.core.model.FileInformation;

@Getter
public class ExtractionFailure {
    FileInformation fileInformation; // file concerned about the failure
    Throwable exception; // exception reason

    public ExtractionFailure(FileInformation fileInformation, Throwable exception) {
        this.fileInformation = fileInformation;
        this.exception = exception;
    }
}
