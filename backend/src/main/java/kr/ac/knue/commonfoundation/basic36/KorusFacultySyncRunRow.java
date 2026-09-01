package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record KorusFacultySyncRunRow(Long runId, String requestId, String runType, LocalDate targetStartDate,
                                     LocalDate targetEndDate, String runStatus, Integer totalCount,
                                     Integer successCount, Integer failureCount, Long createdBy,
                                     LocalDateTime startedAt, LocalDateTime finishedAt, String failureReason) {
}
