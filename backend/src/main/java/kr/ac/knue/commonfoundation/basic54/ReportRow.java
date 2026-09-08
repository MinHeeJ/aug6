package kr.ac.knue.commonfoundation.basic54;

import java.time.LocalDateTime;

public record ReportRow(String reportId, String reportName, String businessCategory, String templateFileRef,
                        String datasetCode, String activeYn, String changeReason, Long createdBy,
                        Long updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
