package kr.ac.knue.commonfoundation.signupimplementation;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.ValidationError;

/** Safe field-aware error crossing the signup boundary without database or credential diagnostics. */
public class SignupFailure extends RuntimeException {
    private final int status;
    private final List<ValidationError> fields;

    public SignupFailure(int status, String field, String message) {
        super(message);
        this.status = status;
        this.fields = field == null ? List.of() : List.of(new ValidationError(field, message));
    }

    public int status() {
        return status;
    }

    public List<ValidationError> fields() {
        return fields;
    }
}
