package kr.ac.knue.commonfoundation.privacy;

public record DecryptionAuditEvent(
        String fieldKey,
        Long actorUserId,
        String purpose,
        DecryptionAuditResult result,
        String failureReason) {
}
