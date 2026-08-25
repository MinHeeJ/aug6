package kr.ac.knue.commonfoundation.privacy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class PrivacyCryptoService {
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final String CIPHERTEXT_PREFIX = "v1";

    private final PrivacyCryptoProperties properties;
    private final DecryptionAuditRecorder auditRecorder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PrivacyCryptoService(PrivacyCryptoProperties properties, DecryptionAuditRecorder auditRecorder) {
        this.properties = properties;
        this.auditRecorder = auditRecorder;
    }

    public String encrypt(String fieldKey, String plaintext) {
        if (!hasText(fieldKey)) {
            throw new IllegalArgumentException("개인정보 필드 키를 입력하세요.");
        }
        if (plaintext == null) {
            throw new IllegalArgumentException("암호화 대상 값을 입력하세요.");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(resolveAesKey(), "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(fieldKey.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return CIPHERTEXT_PREFIX + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(iv) + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new PrivacyCryptoException("암호화 처리에 실패했습니다.", exception);
        }
    }

    public String decrypt(String fieldKey, String ciphertext, Long actorUserId, String purpose) {
        try {
            String plaintext = decryptValue(fieldKey, ciphertext);
            auditRecorder.record(new DecryptionAuditEvent(fieldKey, actorUserId, purpose, DecryptionAuditResult.SUCCESS, null));
            return plaintext;
        } catch (RuntimeException exception) {
            auditRecorder.record(new DecryptionAuditEvent(fieldKey, actorUserId, purpose, DecryptionAuditResult.FAILED, failureReason(exception)));
            throw exception;
        }
    }

    private String decryptValue(String fieldKey, String ciphertext) {
        if (!hasText(fieldKey)) {
            throw new IllegalArgumentException("개인정보 필드 키를 입력하세요.");
        }
        if (!hasText(ciphertext)) {
            throw new IllegalArgumentException("복호화 대상 암호문을 입력하세요.");
        }
        byte[] aesKey = resolveAesKey();
        String[] parts = ciphertext.split(":", -1);
        if (parts.length != 3 || !CIPHERTEXT_PREFIX.equals(parts[0])) {
            throw new PrivacyCryptoException("지원하지 않는 암호문 형식입니다.");
        }
        try {
            byte[] iv = Base64.getUrlDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getUrlDecoder().decode(parts[2]);
            if (iv.length != IV_BYTES) {
                throw new PrivacyCryptoException("지원하지 않는 암호문 형식입니다.");
            }
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(fieldKey.getBytes(StandardCharsets.UTF_8));
            byte[] plain = cipher.doFinal(encrypted);
            return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(plain)).toString();
        } catch (AEADBadTagException exception) {
            throw new PrivacyCryptoException("복호화 처리에 실패했습니다.", exception);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new PrivacyCryptoException("복호화 처리에 실패했습니다.", exception);
        }
    }

    private byte[] resolveAesKey() {
        if (!hasText(properties.getAes256GcmKey())) {
            throw new PrivacyCryptoException("암호화 키가 설정되지 않았습니다.");
        }
        return HmacSearchIdentifierAdapter.normalizeKey(properties.getAes256GcmKey(), "AES-256-GCM");
    }

    private String failureReason(RuntimeException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("키")) {
            return "KEY_MISSING";
        }
        if (message.contains("형식")) {
            return "INVALID_CIPHERTEXT";
        }
        return "DECRYPT_FAILED";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
