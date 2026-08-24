package kr.ac.knue.commonfoundation.fileoperations;

import java.time.LocalDateTime;

public record AttachmentDeleteHistoryRow(
        Long deleteHistoryId,
        Long fileId,
        String deleteMethod,
        String reason,
        Long deletedBy,
        LocalDateTime deletedAt) {
}
