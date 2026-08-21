package kr.ac.knue.commonfoundation.operations;

import java.time.LocalDateTime;

public record DataScopeRuleRow(
        Long dataScopeRuleId,
        String roleCode,
        String roleName,
        String dataScopeType,
        String organizationCode,
        String organizationName,
        String dutyArea,
        String changeReason,
        LocalDateTime updatedAt
) {
}
