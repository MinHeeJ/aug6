package kr.ac.knue.commonfoundation.privacy;

public record PrivacyAccessEvaluateResponse(
        String roleCode,
        String fieldKey,
        String accessType,
        boolean allowed,
        String reason,
        boolean rawValueExposed) {
}
