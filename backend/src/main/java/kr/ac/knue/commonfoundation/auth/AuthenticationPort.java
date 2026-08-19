package kr.ac.knue.commonfoundation.auth;

public interface AuthenticationPort {
    AuthenticatedSession authenticate(LoginRequest request);
}
