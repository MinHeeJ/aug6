package kr.ac.knue.commonfoundation.users;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserRoleSummary(
        Long assignmentId,
        Long userId,
        String roleCode,
        String roleName,
        String assignmentType,
        LocalDate validStartDate,
        LocalDate validEndDate,
        Long approverUserId,
        String status,
        LocalDateTime updatedAt,
        String changeReason
) {
}
