package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;

/** Validation and persistence result for an all-or-nothing employment-rate upload. */
public record EmploymentRateExcelUploadResult(
        String uploadId,
        String originalFileName,
        int totalCount,
        int successCount,
        int errorCount,
        int persistedCount,
        List<EmploymentRateExcelError> errors) {
}
