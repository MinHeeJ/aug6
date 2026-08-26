package kr.ac.knue.commonfoundation.excel;

public record ExcelUploadErrorRow(String errorId, String uploadId, Integer rowNumber, String columnName, String inputValue,
        String errorCode, String errorReason, String correctionGuide) {
}
