package kr.ac.knue.commonfoundation.employmentrateachievements;

/** Result of validating an employment-rate Excel upload before atomic reflection. */
public record EmploymentRateExcelUploadResponse(
        boolean applied,
        int successCount,
        int errorCount,
        String message,
        String errorFileToken) {
}
