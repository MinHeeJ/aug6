package kr.ac.knue.commonfoundation.signup;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Public registration input; secrets are write-only and excluded from diagnostic rendering. */
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
