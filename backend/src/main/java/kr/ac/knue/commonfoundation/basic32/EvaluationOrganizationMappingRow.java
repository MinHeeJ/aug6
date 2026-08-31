package kr.ac.knue.commonfoundation.basic32;

import java.time.LocalDateTime;

public record EvaluationOrganizationMappingRow(
        Long mappingId,
        Long userId,
        String loginId,
        String userName,
        String organizationCode,
        String organizationName,
        String businessType,
        String dataScope,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}
