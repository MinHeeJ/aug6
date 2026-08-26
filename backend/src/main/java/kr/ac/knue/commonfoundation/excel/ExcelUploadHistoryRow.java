package kr.ac.knue.commonfoundation.excel;

import java.time.LocalDateTime;

public record ExcelUploadHistoryRow(String uploadId, String originalFileName, Long uploaderUserId, Integer totalCount,
        Integer successCount, Integer errorCount, Integer excludedCount, Integer savedCount, Long processingTimeMillis,
        LocalDateTime processedAt) {
}
