package kr.ac.knue.commonfoundation.fileoperations;

public record AttachmentDownloadResponse(
        String originalFilename,
        String contentType,
        byte[] content) {
}
