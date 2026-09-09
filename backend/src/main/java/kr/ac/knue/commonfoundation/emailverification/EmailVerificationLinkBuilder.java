package kr.ac.knue.commonfoundation.emailverification;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationLinkBuilder {
    private final String verificationBaseUrl;

    public EmailVerificationLinkBuilder(@Value("${MAIL_VERIFICATION_BASE_URL:}") String verificationBaseUrl) {
        this.verificationBaseUrl = verificationBaseUrl == null ? "" : verificationBaseUrl.trim();
    }

    public String buildVerificationLink(String rawToken, String ignoredHostHeader) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String base = verificationBaseUrl.isBlank() ? "/" : verificationBaseUrl;
        String trimmedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        if (trimmedBase.isBlank()) {
            trimmedBase = "";
        }
        return trimmedBase + "/email-verification?token=" + encodedToken;
    }
}
