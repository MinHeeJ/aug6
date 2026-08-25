package kr.ac.knue.commonfoundation.privacy;

public record PrivacyAccessPermissionSearchCriteria(int page, int size, String roleCode, String fieldKey) {
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
