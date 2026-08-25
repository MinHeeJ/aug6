package kr.ac.knue.commonfoundation.privacy;

public record PrivacyAccessLogRecordRequest(
        String processType,
        Long actorUserId,
        String targetRef,
        String processPurpose,
        String requestIp,
        String processResult,
        String actualValue,
        String originalValue) {
}
