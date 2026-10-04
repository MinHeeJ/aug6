package kr.ac.knue.commonfoundation.courseoperations;

/** Paging parameters for the course-operation list contract. */
public record CourseOperationSearchCriteria(int page, int pageSize) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safePageSize() {
        return pageSize;
    }

    public int offset() {
        return safePage() * safePageSize();
    }
}
