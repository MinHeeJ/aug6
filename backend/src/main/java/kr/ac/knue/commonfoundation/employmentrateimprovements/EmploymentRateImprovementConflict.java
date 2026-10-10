package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Carries a feature conflict code without altering the legacy CONFLICT envelope. */
public class EmploymentRateImprovementConflict extends RuntimeException {
    private final String code;

    public EmploymentRateImprovementConflict(String code) {
        super(code);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
