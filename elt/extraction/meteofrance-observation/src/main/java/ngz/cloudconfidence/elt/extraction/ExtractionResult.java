package ngz.cloudconfidence.elt.extraction;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import ngz.extraction.core.model.FileInformation;
import ngz.extraction.core.sink.SunkFile;

@Getter
public class ExtractionResult {

    List<SunkFile> successes;
    List<ExtractionFailure> failures;

    public ExtractionResult() {
        this.successes = new ArrayList<>();
        this.failures = new ArrayList<>();
    }

    public void addSuccess(SunkFile sunkfile) {
        this.successes.add(sunkfile);
    }

    public void addFailure(FileInformation fileInformation, Throwable exception) {
        this.failures.add(new ExtractionFailure(fileInformation, exception));
    }
}
