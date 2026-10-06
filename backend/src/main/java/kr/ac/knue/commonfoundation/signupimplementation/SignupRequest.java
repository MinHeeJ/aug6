package kr.ac.knue.commonfoundation.signupimplementation;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Create-only signup input; credentials are never serialized or included in diagnostic strings. */
public record SignupRequest(
        String userId,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String passwordConfirm,
        String email
) {
    @Override
    public String toString() {
        return "SignupRequest[redacted]";
    }
}
