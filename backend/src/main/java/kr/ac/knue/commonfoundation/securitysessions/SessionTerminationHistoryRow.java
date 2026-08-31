package kr.ac.knue.commonfoundation.securitysessions;

import java.time.LocalDateTime;

public record SessionTerminationHistoryRow(Long historyId, String sessionId, Long userId, String loginId,
        String employeeNo, String userName, String terminationType, String terminationReason,
        LocalDateTime terminatedAt) {
}
