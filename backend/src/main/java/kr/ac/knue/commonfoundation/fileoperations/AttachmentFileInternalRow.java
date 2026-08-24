package kr.ac.knue.commonfoundation.fileoperations;

import java.time.LocalDateTime;

public record AttachmentFileInternalRow(
        Long fileId,
        String businessType,
        String businessRecordId,
        String businessRecordStatus,
        String originalFilename,
        String storedFilename,
        String storagePath,
        String extension,
        Long fileSizeBytes,
        Long uploadedBy,
        LocalDateTime uploadedAt,
        String malwareScanStatus,
        LocalDateTime deletedAt,
        Long deletedBy,
        String deleteReason) {
    public String storageObjectRef() {
        return storagePath + "/" + storedFilename;
    }
}
