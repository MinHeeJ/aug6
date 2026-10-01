package kr.ac.knue.commonfoundation.achievement;

import java.util.List;
import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorRow;

/** Validation result for the R07 student-guidance template wrapper. */
public record StudentGuidanceExcelUploadResult(
        String uploadId, String originalFileName, String validationStatus, int totalCount,
        int successCount, int errorCount, List<ExcelUploadErrorRow> errors
) {
}
