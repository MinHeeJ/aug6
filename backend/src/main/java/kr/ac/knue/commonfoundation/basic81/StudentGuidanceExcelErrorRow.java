package kr.ac.knue.commonfoundation.basic81;

/** User-visible Excel validation error without exposing file storage internals. */
public record StudentGuidanceExcelErrorRow(
        int rowNumber,
        String columnName,
        String inputValue,
        String errorCode,
        String errorReason,
        String correctionGuide) {
}
