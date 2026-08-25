package kr.ac.knue.commonfoundation.privacy;

@FunctionalInterface
public interface DecryptionAuditRecorder {
    void record(DecryptionAuditEvent event);
}
