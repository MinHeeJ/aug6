package kr.ac.knue.commonfoundation.signup;

/** Registration acknowledgement, intentionally containing no internal identity or credentials. */
public record SignupResponse(String userId, String message) {
}
