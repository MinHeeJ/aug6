package kr.ac.knue.commonfoundation.users;

public record UserSearchCriteria(
        int page,
        int size,
        String employeeNo,
        String name,
        String organizationCodeFilter,
        String rankName,
        String employmentStatus,
        String roleCodeFilter,
        String systemUseYn,
        String filter
) {
    public int offset() {
        return Math.max(0, page) * Math.max(1, size);
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
