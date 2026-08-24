package kr.ac.knue.commonfoundation.fileoperations;

import java.time.LocalDateTime;

public record AttachmentIntegrityFindingRow(
        Long findingId,
        Long checkId,
        Long fileId,
        String storageObjectRef,
        String anomalyType,
        String resultMessage,
        LocalDateTime createdAt) {
}
