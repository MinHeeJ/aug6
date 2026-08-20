package kr.ac.knue.commonfoundation.permissions;

public record MenuPermissionSearchCriteria(int page, int size, String targetType, String targetId, String filter, String accessAllowed) {
    public int safeSize() {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }
}
