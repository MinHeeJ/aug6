package kr.ac.knue.commonfoundation.mail;

public record VerificationMailCommand(Long userId, String loginId, String email, String verificationToken, String requestId) {
    public VerificationMailCommand {
        if (email == null || email.trim().isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (verificationToken == null || verificationToken.trim().isBlank()) {
            throw new IllegalArgumentException("verificationToken is required");
        }
    }
}
