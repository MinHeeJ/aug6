package kr.ac.knue.commonfoundation.privacy;

import java.time.LocalDateTime;

public record PrivacyAccessLogRow(
        Long historyId,
        String processType,
        Long actorUserId,
        String actorLoginId,
        String targetRef,
        String processPurpose,
        LocalDateTime processedAt,
        String requestIp,
        String processResult) {
}
