package kr.ac.knue.commonfoundation.emailverification;

import org.springframework.stereotype.Repository;

@Repository
public class MailDeliveryAttemptRepository {
    private final MailDeliveryAttemptMapper mapper;

    public MailDeliveryAttemptRepository(MailDeliveryAttemptMapper mapper) {
        this.mapper = mapper;
    }

    public Long recordRequested(Long userId, String email) {
        return recordRequested(userId, email, null);
    }

    public Long recordRequested(Long userId, String email, String requesterIp) {
        String normalizedEmail = EmailAddressPolicy.normalizeEmail(email);
        EmailAddressPolicy.validateSensitiveMailTarget(normalizedEmail);
        MailDeliveryAttemptMapper.NewMailDeliveryAttempt attempt = new MailDeliveryAttemptMapper.NewMailDeliveryAttempt(
                userId,
                normalizedEmail,
                normalizeBlankToNull(requesterIp)
        );
        mapper.insertRequested(attempt);
        return attempt.getAttemptId();
    }

    public MailDeliveryAttempt findById(Long attemptId) {
        return mapper.findById(attemptId);
    }

    public int markFailure(Long attemptId, String deliveryStatus, String diagnosticCategory, String redactedDiagnostic, boolean retryEligible) {
        return mapper.markFailure(attemptId, deliveryStatus, diagnosticCategory, redactedDiagnostic, retryEligible ? "Y" : "N");
    }

    public int markSent(Long attemptId) {
        return mapper.markSent(attemptId);
    }

    public int markAuthFailure(Long attemptId, String redactedDiagnostic) {
        return mapper.markFailure(attemptId, "FAILED_AUTH", "AUTH", redactedDiagnostic, "Y");
    }

    public int markTimeoutFailure(Long attemptId, String redactedDiagnostic) {
        return mapper.markFailure(attemptId, "FAILED_TIMEOUT", "TIMEOUT", redactedDiagnostic, "Y");
    }

    public int markRateLimitFailure(Long attemptId, String redactedDiagnostic) {
        return mapper.markFailure(attemptId, "FAILED_RATE_LIMIT", "RATE_LIMIT", redactedDiagnostic, "Y");
    }

    public int markOtherFailure(Long attemptId, String redactedDiagnostic, boolean retryEligible) {
        return mapper.markFailure(attemptId, "FAILED_OTHER", "UNKNOWN", redactedDiagnostic, retryEligible ? "Y" : "N");
    }

    public MailDeliveryAttempt findMostRecentByEmail(String email) {
        return mapper.findMostRecentByEmail(EmailAddressPolicy.normalizeEmail(email));
    }

    public boolean hasRecentAttemptInsideThrottleWindow(String email) {
        return mapper.countRecentAttemptsForEmailThrottle(EmailAddressPolicy.normalizeEmail(email)) > 0;
    }

    public boolean hasRecentAttemptInsideThrottleWindow(Long userId, String email) {
        return hasRecentAttemptInsideThrottleWindow(userId, email, 60);
    }

    public boolean hasRecentAttemptInsideThrottleWindow(Long userId, String email, int seconds) {
        return mapper.countRecentAttemptsForThrottle(userId, EmailAddressPolicy.normalizeEmail(email), seconds) > 0;
    }

    public int countAttemptsSince(String email, String requesterIp, int seconds) {
        String normalizedEmail = email == null || email.isBlank() ? null : EmailAddressPolicy.normalizeEmail(email);
        return mapper.countAttemptsSince(normalizedEmail, normalizeBlankToNull(requesterIp), seconds);
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
