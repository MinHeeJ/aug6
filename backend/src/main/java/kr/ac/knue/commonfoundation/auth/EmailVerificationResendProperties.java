package kr.ac.knue.commonfoundation.auth;

public record EmailVerificationResendProperties(
        int sameAccountEmailThrottleSeconds,
        int maxAttemptsPerEmailPerHour,
        int maxAttemptsPerIpPerHour
) {
    public EmailVerificationResendProperties {
        if (sameAccountEmailThrottleSeconds <= 0) {
            sameAccountEmailThrottleSeconds = 60;
        }
        if (maxAttemptsPerEmailPerHour <= 0) {
            maxAttemptsPerEmailPerHour = 5;
        }
        if (maxAttemptsPerIpPerHour <= 0) {
            maxAttemptsPerIpPerHour = 20;
        }
    }
}
