package kr.ac.knue.commonfoundation.temporarypermissions;

public record TemporaryPermissionSearchCriteria(
        int page,
        int size,
        Long userId) {
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
