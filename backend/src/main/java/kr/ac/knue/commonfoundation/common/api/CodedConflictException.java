package kr.ac.knue.commonfoundation.common.api;

/** Signals a business conflict whose stable error code lets clients distinguish locks from generic conflicts. */
public class CodedConflictException extends RuntimeException {
    private final String code;

    public CodedConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
