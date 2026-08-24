package kr.ac.knue.commonfoundation.permissionops;

public record PermissionChangeHistorySearchCriteria(
        int page,
        int size,
        String targetType,
        String targetId
) {
    public PermissionChangeHistorySearchCriteria normalized() {
        return new PermissionChangeHistorySearchCriteria(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                normalizeText(targetType),
                normalizeText(targetId));
    }

    public int offset() {
        return Math.max(page, 0) * Math.max(size, 1);
    }

    private static String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
