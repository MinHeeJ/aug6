package kr.ac.knue.commonfoundation.basic36;

public record ResearcherLookupCriteria(int page, int size, String keyword) {
    public int safeSize() {
        return size == 50 || size == 100 ? size : 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedKeyword() {
        return keyword != null && !keyword.isBlank() ? keyword.trim() : null;
    }
}
