package kr.ac.knue.commonfoundation.privacy;

import org.springframework.stereotype.Component;

@Component
public class NoopDecryptionAuditRecorder implements DecryptionAuditRecorder {
    @Override
    public void record(DecryptionAuditEvent event) {
    }
}
