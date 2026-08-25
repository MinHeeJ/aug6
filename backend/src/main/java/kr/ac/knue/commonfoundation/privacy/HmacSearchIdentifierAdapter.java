package kr.ac.knue.commonfoundation.privacy;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class HmacSearchIdentifierAdapter {
    private static final String ALGORITHM = "HmacSHA256";
    private static final String DELIMITER = "\u001F";

    private final PrivacyCryptoProperties properties;

    public HmacSearchIdentifierAdapter(PrivacyCryptoProperties properties) {
        this.properties = properties;
    }

    public String identifier(String fieldKey, String plaintext) {
        if (!hasText(fieldKey)) {
            throw new IllegalArgumentException("개인정보 필드 키를 입력하세요.");
        }
        if (plaintext == null) {
            throw new IllegalArgumentException("검색 식별자 생성 대상 값을 입력하세요.");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(resolveHmacKey(), ALGORITHM));
            byte[] digest = mac.doFinal((fieldKey + DELIMITER + plaintext).getBytes(StandardCharsets.UTF_8));
            return "hmac-sha256:" + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new PrivacyCryptoException("검색 식별자 생성에 실패했습니다.", exception);
        }
    }

    private byte[] resolveHmacKey() {
        String key = hasText(properties.getHmacKey()) ? properties.getHmacKey() : properties.getAes256GcmKey();
        if (!hasText(key)) {
            throw new PrivacyCryptoException("HMAC 키가 설정되지 않았습니다.");
        }
        return normalizeKey(key, "HMAC");
    }

    static byte[] normalizeKey(String configuredKey, String keyName) {
        String trimmedKey = configuredKey == null ? "" : configuredKey.trim();
        if (!hasText(trimmedKey)) {
            throw new PrivacyCryptoException(keyName + " 키가 설정되지 않았습니다.");
        }
        byte[] rawKeyBytes = trimmedKey.getBytes(StandardCharsets.UTF_8);
        if (rawKeyBytes.length == 32) {
            return rawKeyBytes;
        }
        byte[] decoded = tryBase64Decode(trimmedKey);
        byte[] keyBytes = decoded == null ? rawKeyBytes : decoded;
        if (keyBytes.length == 32) {
            return keyBytes;
        }
        if (keyBytes.length > 32) {
            try {
                return MessageDigest.getInstance("SHA-256").digest(keyBytes);
            } catch (NoSuchAlgorithmException exception) {
                throw new PrivacyCryptoException(keyName + " 키 정규화에 실패했습니다.", exception);
            }
        }
        throw new PrivacyCryptoException(keyName + " 키는 32바이트 이상이어야 합니다.");
    }

    private static byte[] tryBase64Decode(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
