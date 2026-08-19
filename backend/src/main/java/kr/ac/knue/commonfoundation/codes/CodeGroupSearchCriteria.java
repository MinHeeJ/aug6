package kr.ac.knue.commonfoundation.codes;

public record CodeGroupSearchCriteria(int page, int size, String groupIdFilter, String filter) {
    public int safeSize() {
        if (size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }
}
