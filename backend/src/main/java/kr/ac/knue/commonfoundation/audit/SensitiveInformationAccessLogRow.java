package kr.ac.knue.commonfoundation.audit;

import java.time.LocalDateTime;

public record SensitiveInformationAccessLogRow(
        Long accessLogId,
        String informationType,
        Long viewerUserId,
        String viewerLoginId,
        String viewerName,
        String targetScope,
        String accessPurpose,
        String purposeSource,
        String accessResult,
        String requestId,
        LocalDateTime accessedAt) {
}
