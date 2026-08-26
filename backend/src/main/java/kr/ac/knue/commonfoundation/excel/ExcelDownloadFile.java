package kr.ac.knue.commonfoundation.excel;

public record ExcelDownloadFile(String originalFileName, String contentType, byte[] content) {
}
