package kr.ac.knue.commonfoundation.auth;

public record AuthenticatedSession(String sessionId, CurrentUser user) {
}
