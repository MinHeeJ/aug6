package kr.ac.knue.commonfoundation.fileoperations;

public record AttachmentUploadCandidate(
        String originalFilename,
        String extension,
        long fileSizeBytes) {
}
