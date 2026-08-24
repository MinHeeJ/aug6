package kr.ac.knue.commonfoundation.fileoperations;

public record AttachmentIntegrityExcelDownload(String filename, String contentType, byte[] content) {
}
