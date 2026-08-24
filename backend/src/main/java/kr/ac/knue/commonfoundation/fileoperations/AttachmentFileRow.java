package kr.ac.knue.commonfoundation.fileoperations;

import java.time.LocalDateTime;

public record AttachmentFileRow(
        Long fileId,
        String businessType,
        String businessRecordId,
        String businessRecordStatus,
        String originalFilename,
        String extension,
        Long fileSizeBytes,
        Long uploadedBy,
        LocalDateTime uploadedAt,
        String malwareScanStatus,
        LocalDateTime deletedAt) {
}
