package kr.ac.knue.commonfoundation.privacy;

public record PrivacyAccessLogSearchCriteria(
        int page,
        int size,
        Long actorUserId,
        String targetRef,
        String processType,
        String processedFrom,
        String processedTo) {
    public int safeSize() {
        return switch (size) {
            case 50 -> 50;
            case 100 -> 100;
            default -> 20;
        };
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }
}
