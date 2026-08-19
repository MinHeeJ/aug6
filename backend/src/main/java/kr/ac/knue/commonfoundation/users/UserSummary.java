package kr.ac.knue.commonfoundation.users;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UserSummary(
        Long userId,
        String loginId,
        String employeeNo,
        String name,
        String organizationCode,
        String organizationName,
        String rankName,
        String employmentStatus,
        String positionName,
        LocalDate retirementDate,
        LocalDateTime lastSyncedAt,
        String systemUseYn,
        String status,
        List<String> roleCodes
) {
}
