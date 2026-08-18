package kr.ac.knue.commonfoundation.roles;

import java.time.LocalDateTime;

public record RoleRow(
        String roleCode,
        String roleName,
        String purpose,
        String assignmentCriteria,
        String defaultDataScope,
        String systemUseYn,
        String status,
        LocalDateTime updatedAt
) {
}
