package kr.ac.knue.commonfoundation.privacy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "privacy.crypto")
public class PrivacyCryptoProperties {
    private String aes256GcmKey;
    private String hmacKey;

    public String getAes256GcmKey() {
        return aes256GcmKey;
    }

    public void setAes256GcmKey(String aes256GcmKey) {
        this.aes256GcmKey = aes256GcmKey;
    }

    public String getHmacKey() {
        return hmacKey;
    }

    public void setHmacKey(String hmacKey) {
        this.hmacKey = hmacKey;
    }
}
