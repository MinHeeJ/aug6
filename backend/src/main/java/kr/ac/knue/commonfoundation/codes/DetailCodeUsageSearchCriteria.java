package kr.ac.knue.commonfoundation.codes;

public record DetailCodeUsageSearchCriteria(String groupId, int page, int size) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safeSize() {
        return size <= 0 ? 10 : Math.min(size, 100);
    }

    public int offset() {
        return safePage() * safeSize();
    }
}
