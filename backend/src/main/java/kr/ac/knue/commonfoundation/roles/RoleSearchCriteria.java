package kr.ac.knue.commonfoundation.roles;

public record RoleSearchCriteria(int page, int size, String filter) {
    public int offset() {
        return Math.max(0, page) * safeSize();
    }

    public int getOffset() {
        return offset();
    }

    public int safeSize() {
        return Math.max(1, Math.min(size, 100));
    }

    public int getSafeSize() {
        return safeSize();
    }
}
