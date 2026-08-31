package kr.ac.knue.commonfoundation.securitysessions;

import java.time.LocalDateTime;

public record ActiveSessionRow(String sessionId, Long userId, String loginId, String employeeNo, String userName,
        LocalDateTime loginAt, LocalDateTime lastAccessedAt, String ipAddress, String status,
        Long terminatedBy, LocalDateTime terminatedAt, String terminationReason) {
}
