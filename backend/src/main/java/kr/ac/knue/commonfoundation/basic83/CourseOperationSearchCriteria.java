package kr.ac.knue.commonfoundation.basic83;

/** Captures paging for course-operation rows without null-bound SQL predicates. */
public record CourseOperationSearchCriteria(int page, int pageSize) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safePageSize() {
        return pageSize == 50 || pageSize == 100 ? pageSize : 20;
    }

    public int offset() {
        return safePage() * safePageSize();
    }
}
