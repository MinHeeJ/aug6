package kr.ac.knue.commonfoundation.basic54;

import java.time.LocalDateTime;

public record ReportPermissionRow(Long permissionId, String granteeType, String granteeId, String reportId,
                                  String reportName, String allowViewYn, String allowPreviewYn, String allowPrintYn,
                                  String allowPdfYn, String allowExcelYn, String dataScope, String activeYn,
                                  String changeReason, Long createdBy, Long updatedBy,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
}
