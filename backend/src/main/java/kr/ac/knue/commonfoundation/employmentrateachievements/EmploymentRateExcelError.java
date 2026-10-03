package kr.ac.knue.commonfoundation.employmentrateachievements;

/** A row-level upload diagnostic retained when the entire import is rejected. */
public record EmploymentRateExcelError(
        int rowNumber,
        String columnName,
        String inputValue,
        String errorCode,
        String errorReason) {
}
