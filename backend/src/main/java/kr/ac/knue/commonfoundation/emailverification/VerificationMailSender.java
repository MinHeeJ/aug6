package kr.ac.knue.commonfoundation.emailverification;

public interface VerificationMailSender {
    void sendVerificationMail(String normalizedEmail, String rawToken);

    default void sendMailhogFallback(String normalizedEmail, String rawToken) {
        throw new UnsupportedOperationException("MailHog fallback is not allowed for Gmail verification mail");
    }
}
