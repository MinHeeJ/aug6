package kr.ac.knue.commonfoundation.mail;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.mail.verification")
public class VerificationMailProperties {
    private String from = "noreply@example.edu";
    private String baseUrl = "http://localhost:3000";
    private String subject = "이메일 인증을 완료해주세요";

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean isAllowedVerificationBaseUrl() {
        String normalized = baseUrl == null ? "" : baseUrl.toLowerCase(Locale.ROOT);
        return normalized.startsWith("https://") || normalized.startsWith("http://localhost") || normalized.startsWith("http://127.0.0.1");
    }
}
