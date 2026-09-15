package kr.ac.knue.commonfoundation.common.api;

/** A conflict with a stable, client-actionable business error code. */
public class CodedConflictException extends ConflictException {
    private final String code;

    public CodedConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
