package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDateTime;

/** R07-facing upload history summary for the student-guidance Excel wizard. */
public record StudentGuidanceExcelHistoryRow(
        String uploadId,
        String originalFileName,
        int totalCount,
        int successCount,
        int errorCount,
        int savedCount,
        LocalDateTime processedAt) {
}
