package kr.ac.knue.commonfoundation.userroles;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserRoleAssignmentSummary(
        Long assignmentId,
        Long userId,
        String loginId,
        String employeeNo,
        String name,
        String roleCode,
        String roleName,
        String assignmentType,
        LocalDate validStartDate,
        LocalDate validEndDate,
        Long approverUserId,
        String approverName,
        String status,
        LocalDateTime revokedAt,
        Long revokedBy,
        LocalDateTime updatedAt,
        String changeReason
) {
}
