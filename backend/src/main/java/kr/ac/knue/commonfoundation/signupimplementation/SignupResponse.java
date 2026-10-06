package kr.ac.knue.commonfoundation.signupimplementation;

/** Public signup receipt intentionally omits internal keys, email and credential material. */
public record SignupResponse(String userId, String message) {
}
