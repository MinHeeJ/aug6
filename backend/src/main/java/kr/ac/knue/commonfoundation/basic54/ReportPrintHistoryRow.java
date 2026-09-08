package kr.ac.knue.commonfoundation.basic54;

import java.time.LocalDateTime;

public record ReportPrintHistoryRow(Long printHistoryId, String reportId, String reportName, Long requesterId,
                                    String requesterName, String targetSummary, String outputFormat,
                                    int outputCount, String resultCode, LocalDateTime outputAt, String fileRef,
                                    String requestId) {
}
