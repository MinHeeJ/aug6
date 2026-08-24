package kr.ac.knue.commonfoundation.functionpermissions;

public record FunctionPermissionSearchCriteria(int page, int size, String screenId, String roleCode) {
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
