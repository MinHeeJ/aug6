package kr.ac.knue.commonfoundation.securitysessions;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class TerminateActiveSessionRequest {
    private String reason;
    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Set<String> getUnexpectedFields() {
        return Collections.unmodifiableSet(unexpectedFields);
    }

    @JsonAnySetter
    void rejectUnexpected(String field, Object value) {
        unexpectedFields.add(field);
    }
}
