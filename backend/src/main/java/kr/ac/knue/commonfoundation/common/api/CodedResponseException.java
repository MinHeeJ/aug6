package kr.ac.knue.commonfoundation.common.api;

import java.util.List;
import org.springframework.http.HttpStatus;

public class CodedResponseException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final List<ValidationError> fields;

    public CodedResponseException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    public CodedResponseException(HttpStatus status, String code, String message, List<ValidationError> fields) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = List.copyOf(fields);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public List<ValidationError> fields() {
        return fields;
    }
}
