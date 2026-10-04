package kr.ac.knue.commonfoundation.courseoperations;

/** Captures course-operation pagination while keeping SQL predicates value-driven. */
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
