package kr.ac.knue.commonfoundation.auth;

/**
 * Public registration result that intentionally excludes credentials and session information.
 */
public record SignupResponse(String userId, String message) {
}
