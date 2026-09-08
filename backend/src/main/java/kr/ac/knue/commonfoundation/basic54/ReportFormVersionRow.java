package kr.ac.knue.commonfoundation.basic54;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportFormVersionRow(Long formVersionId, String reportId, String reportName, String versionName,
                                   LocalDate effectiveDate, String formFileRef, String currentYn,
                                   String changeReason, Long createdBy, Long updatedBy,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
}
