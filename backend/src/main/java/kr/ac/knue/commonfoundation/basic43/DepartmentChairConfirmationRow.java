package kr.ac.knue.commonfoundation.basic43;

import java.time.LocalDateTime;

public record DepartmentChairConfirmationRow(
        Long confirmationId,
        Long achievementId,
        String evaluationYear,
        String departmentOrganizationCode,
        String areaCode,
        String confirmStatus,
        String previousStatus,
        String nextStatus,
        String opinion,
        String reasonCode,
        Long processedBy,
        LocalDateTime processedAt,
        String changeReason) {
}
