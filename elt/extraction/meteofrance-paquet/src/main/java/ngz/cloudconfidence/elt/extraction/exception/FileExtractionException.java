package ngz.cloudconfidence.elt.extraction.exception;

import java.util.List;
import lombok.Getter;
import ngz.cloudconfidence.elt.extraction.ExtractionFailure;
import ngz.extraction.core.model.FileInformation;

@Getter
public class FileExtractionException extends Exception {
    private final List<FileInformation> failedFiles;

    public FileExtractionException(String message, List<ExtractionFailure> failures) {
        super(message);
        this.failedFiles = failures.stream().map(ExtractionFailure::getFileInformation).toList();

        // Attach every individual exception to this root exception
        for (ExtractionFailure failure : failures) {
            if (failure.getException() != null) {
                this.addSuppressed(failure.getException());
            }
        }
    }
}
