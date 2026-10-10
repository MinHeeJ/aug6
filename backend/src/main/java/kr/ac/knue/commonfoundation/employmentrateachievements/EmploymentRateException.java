package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.Map;

/** Feature-only structured failure, preserving the legacy conflict envelope elsewhere. */
public class EmploymentRateException extends RuntimeException {
    final String code;
    final int status;
    final Map<String, Object> details;

    public EmploymentRateException(String code, String message, int status) {
        this(code, message, status, Map.of());
    }

    public EmploymentRateException(String code, String message, int status, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }
}
