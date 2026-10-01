package kr.ac.knue.commonfoundation.privacy;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class NoopDecryptionAuditRecorder implements DecryptionAuditRecorder {
    @Override
    public void record(DecryptionAuditEvent event) {
    }
}
