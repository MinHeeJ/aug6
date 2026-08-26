package kr.ac.knue.commonfoundation.excel;

import java.util.List;

public record ExcelUploadResult(String uploadId, String businessType, String originalFileName, String validationStatus,
        int totalCount, int successCount, int errorCount, int excludedCount, int savedCount, List<ExcelUploadErrorRow> errors) {
}
