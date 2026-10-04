package kr.ac.knue.commonfoundation.lectureimprovements;

/** Captures pagination values while keeping SQL predicates independent of null filter bindings. */
public record LectureImprovementSearchCriteria(int page, int pageSize) {
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
