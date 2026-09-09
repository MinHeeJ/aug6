package kr.ac.knue.commonfoundation.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.email-verification.resend")
public class EmailVerificationResendSettings {
    private int sameAccountEmailThrottleSeconds = 60;
    private int maxAttemptsPerEmailPerHour = 5;
    private int maxAttemptsPerIpPerHour = 20;

    public int getSameAccountEmailThrottleSeconds() {
        return sameAccountEmailThrottleSeconds;
    }

    public void setSameAccountEmailThrottleSeconds(int sameAccountEmailThrottleSeconds) {
        this.sameAccountEmailThrottleSeconds = sameAccountEmailThrottleSeconds;
    }

    public int getMaxAttemptsPerEmailPerHour() {
        return maxAttemptsPerEmailPerHour;
    }

    public void setMaxAttemptsPerEmailPerHour(int maxAttemptsPerEmailPerHour) {
        this.maxAttemptsPerEmailPerHour = maxAttemptsPerEmailPerHour;
    }

    public int getMaxAttemptsPerIpPerHour() {
        return maxAttemptsPerIpPerHour;
    }

    public void setMaxAttemptsPerIpPerHour(int maxAttemptsPerIpPerHour) {
        this.maxAttemptsPerIpPerHour = maxAttemptsPerIpPerHour;
    }

    public EmailVerificationResendProperties toProperties() {
        return new EmailVerificationResendProperties(
                sameAccountEmailThrottleSeconds,
                maxAttemptsPerEmailPerHour,
                maxAttemptsPerIpPerHour
        );
    }
}
