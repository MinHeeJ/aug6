package kr.ac.knue.commonfoundation.menus;

public record MenuUsageSearchCriteria(int page, int size, String filter, String systemUseYn) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safeSize() {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    public int offset() {
        return safePage() * safeSize();
    }
}
