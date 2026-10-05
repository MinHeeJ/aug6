package kr.ac.knue.commonfoundation.employmentrateachievements;

/** Validation outcome for an employment-rate Excel upload request. */
public record EmploymentRateExcelUploadResponse(
        String uploadId,
        String originalFileName,
        int totalCount,
        int successCount,
        int errorCount,
        String message) {
}
