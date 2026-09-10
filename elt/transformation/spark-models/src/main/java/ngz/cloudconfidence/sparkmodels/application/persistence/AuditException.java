package ngz.cloudconfidence.sparkmodels.application.persistence;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class AuditException extends Exception {

    private final List<AuditViolation> violations;

    /** Single-violation constructor — most common case. */
    public AuditException(AuditViolation violation) {
        super(violation.toString());
        this.violations = Collections.singletonList(violation);
    }

    /** Multi-violation constructor — fail-all-checks-before-throwing pattern. */
    public AuditException(List<AuditViolation> violations) {
        super(buildMessage(violations));
        this.violations = Collections.unmodifiableList(violations);
    }

    public List<AuditViolation> getViolations() {
        return violations;
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    private static String buildMessage(List<AuditViolation> violations) {
        StringJoiner sj =
                new StringJoiner(
                        "\n  - ",
                        "Audit failed with " + violations.size() + " violation(s):\n  - ",
                        "");
        violations.forEach(v -> sj.add(v.toString()));
        return sj.toString();
    }
}
