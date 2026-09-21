package kr.ac.knue.commonfoundation.common.api;

/**
 * Signals a request that is valid in shape but conflicts with protected business state.
 *
 * <p>The optional code lets a domain expose a stable, machine-readable conflict reason while
 * preserving the generic {@code CONFLICT} response used by existing callers.
 */
public class ConflictException extends RuntimeException {
    private final String code;

    public ConflictException(String message) {
        this("CONFLICT", message);
    }

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
