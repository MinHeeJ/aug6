package kr.ac.knue.commonfoundation.periodpermissions;

public record PeriodPermissionSearchCriteria(int page, int size, String businessPeriodId) {
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
