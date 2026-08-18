package kr.ac.knue.commonfoundation.common.api;

import java.util.List;

public class BusinessValidationException extends RuntimeException {
    private final List<ValidationError> fields;

    public BusinessValidationException(String message, List<ValidationError> fields) {
        super(message);
        this.fields = fields;
    }

    public List<ValidationError> fields() {
        return fields;
    }
}
