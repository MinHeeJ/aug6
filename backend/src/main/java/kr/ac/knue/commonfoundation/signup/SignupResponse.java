package kr.ac.knue.commonfoundation.signup;

public record SignupResponse(String loginId, String email, String accountStatus, String message, boolean resendAvailable) {
}
