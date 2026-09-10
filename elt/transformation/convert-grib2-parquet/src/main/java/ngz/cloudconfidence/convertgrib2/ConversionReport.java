package ngz.cloudconfidence.convertgrib2;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ConversionReport {

    List<ProcessResult> successes;
    List<ProcessResult> failures;

    public ConversionReport() {
        this.successes = new ArrayList<>();
        this.failures = new ArrayList<>();
    }

    public void addSuccess(ProcessResult processResult) {
        this.successes.add(processResult);
    }

    public void addFailure(ProcessResult processResult) {
        this.failures.add(processResult);
    }

    public static ConversionReport fromProcessResults(List<ProcessResult> results) {
        ConversionReport conversionReport = new ConversionReport();

        for (ProcessResult result : results) {

            if (result.isSuccess()) {

                conversionReport.addSuccess(result);
            } else {
                conversionReport.addFailure(result);
            }
        }

        return conversionReport;
    }
}
