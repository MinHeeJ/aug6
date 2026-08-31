package kr.ac.knue.commonfoundation.basic32;

public record BusinessStatusTransitionSearchCriteria(
        int page,
        int size,
        String businessType,
        String fromStatusCode,
        String executorRoleCode) {
    public int safeSize() {
        if (size != 20 && size != 50 && size != 100) {
            return 20;
        }
        return size;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }
}
