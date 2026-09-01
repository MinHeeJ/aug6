package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDateTime;

public record KorusFacultySyncResultRow(Long resultId, Long runId, String requestId, String employeeNo, String name,
                                        String organizationCode, String rankName, String appointmentId,
                                        String syncStatus, String errorMessage, Long retryOfResultId,
                                        LocalDateTime createdAt) {
}
