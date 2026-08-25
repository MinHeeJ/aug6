package kr.ac.knue.commonfoundation.privacy;

import java.time.LocalDateTime;

public record PrivacyFieldPolicyRow(
        Long policyId,
        String fieldKey,
        String privacyGrade,
        String encryptionRequiredYn,
        String maskingRule,
        String logExclusionYn,
        String changeReason,
        LocalDateTime updatedAt,
        Long updatedBy) {
}
