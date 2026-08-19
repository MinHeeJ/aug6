package kr.ac.knue.commonfoundation.codes;

public record DetailCodeSearchCriteria(String groupId, String filter, int page, int size) {
    public String getGroupId() {
        return groupId;
    }

    public String getFilter() {
        return filter;
    }

    public int safeSize() {
        if (size <= 0) return 20;
        return Math.min(size, 100);
    }

    public int getSafeSize() {
        return safeSize();
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public int getOffset() {
        return offset();
    }
}
