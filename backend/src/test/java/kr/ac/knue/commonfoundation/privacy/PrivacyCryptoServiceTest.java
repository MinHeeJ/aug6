package kr.ac.knue.commonfoundation.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrivacyCryptoServiceTest {
    private static final String AES_KEY = "0123456789abcdef0123456789abcdef";
    private static final String HMAC_KEY = "abcdef0123456789abcdef0123456789";

    @Test
    void encryptDoesNotExposePlaintextAndDecryptRecordsSuccessfulAuditForReq194() {
        CapturingAuditRecorder auditRecorder = new CapturingAuditRecorder();
        PrivacyCryptoService service = new PrivacyCryptoService(properties(AES_KEY, HMAC_KEY), auditRecorder);

        String ciphertext = service.encrypt("researcher_registration_no", "protected-value");
        String plaintext = service.decrypt("researcher_registration_no", ciphertext, 1L, "업무 조회");

        assertThat(ciphertext).startsWith("v1:");
        assertThat(ciphertext).doesNotContain("protected-value");
        assertThat(plaintext).isEqualTo("protected-value");
        assertThat(auditRecorder.events).hasSize(1);
        assertThat(auditRecorder.events.get(0).fieldKey()).isEqualTo("researcher_registration_no");
        assertThat(auditRecorder.events.get(0).actorUserId()).isEqualTo(1L);
        assertThat(auditRecorder.events.get(0).purpose()).isEqualTo("업무 조회");
        assertThat(auditRecorder.events.get(0).result()).isEqualTo(DecryptionAuditResult.SUCCESS);
        assertThat(auditRecorder.events.get(0).failureReason()).isNull();
    }

    @Test
    void decryptFailsAndRecordsAuditWhenKeyIsMissingForReq194() {
        CapturingAuditRecorder auditRecorder = new CapturingAuditRecorder();
        PrivacyCryptoService service = new PrivacyCryptoService(properties("", HMAC_KEY), auditRecorder);

        assertThatThrownBy(() -> service.decrypt("researcher_registration_no", "v1:missing:key", 1L, "업무 조회"))
                .isInstanceOf(PrivacyCryptoException.class)
                .hasMessageContaining("암호화 키");
        assertThat(auditRecorder.events).hasSize(1);
        assertThat(auditRecorder.events.get(0).result()).isEqualTo(DecryptionAuditResult.FAILED);
        assertThat(auditRecorder.events.get(0).failureReason()).contains("KEY_MISSING");
    }

    @Test
    void decryptFailsAndRecordsAuditWhenCiphertextIsTamperedForReq194() {
        CapturingAuditRecorder auditRecorder = new CapturingAuditRecorder();
        PrivacyCryptoService service = new PrivacyCryptoService(properties(AES_KEY, HMAC_KEY), auditRecorder);
        String ciphertext = service.encrypt("researcher_registration_no", "protected-value");
        String tamperedCiphertext = ciphertext.substring(0, ciphertext.length() - 2) + "AA";

        assertThatThrownBy(() -> service.decrypt("researcher_registration_no", tamperedCiphertext, 1L, "업무 조회"))
                .isInstanceOf(PrivacyCryptoException.class)
                .hasMessageContaining("복호화");
        assertThat(auditRecorder.events).hasSize(1);
        assertThat(auditRecorder.events.get(0).result()).isEqualTo(DecryptionAuditResult.FAILED);
        assertThat(auditRecorder.events.get(0).failureReason()).contains("DECRYPT_FAILED");
    }

    @Test
    void hmacSearchIdentifierIsDeterministicFieldScopedAndDoesNotContainPlaintextForReq194() {
        HmacSearchIdentifierAdapter adapter = new HmacSearchIdentifierAdapter(properties(AES_KEY, HMAC_KEY));

        String first = adapter.identifier("researcher_registration_no", "protected-value");
        String second = adapter.identifier("researcher_registration_no", "protected-value");
        String otherField = adapter.identifier("account_no", "protected-value");

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(otherField);
        assertThat(first).startsWith("hmac-sha256:");
        assertThat(first).doesNotContain("protected-value");
    }

    private PrivacyCryptoProperties properties(String aesKey, String hmacKey) {
        PrivacyCryptoProperties properties = new PrivacyCryptoProperties();
        properties.setAes256GcmKey(aesKey);
        properties.setHmacKey(hmacKey);
        return properties;
    }

    private static class CapturingAuditRecorder implements DecryptionAuditRecorder {
        private final List<DecryptionAuditEvent> events = new ArrayList<>();

        @Override
        public void record(DecryptionAuditEvent event) {
            events.add(event);
        }
    }
}
