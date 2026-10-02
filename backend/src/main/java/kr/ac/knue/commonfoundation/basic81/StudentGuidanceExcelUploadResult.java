package kr.ac.knue.commonfoundation.basic81;

import java.util.List;

/** Result of validating a student-guidance Excel file before any domain commit. */
public record StudentGuidanceExcelUploadResult(
        String uploadId,
        String originalFileName,
        int totalCount,
        int successCount,
        int errorCount,
        List<StudentGuidanceExcelErrorRow> errors) {
}
