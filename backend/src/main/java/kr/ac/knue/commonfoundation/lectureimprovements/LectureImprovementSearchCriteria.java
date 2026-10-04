package kr.ac.knue.commonfoundation.lectureimprovements;

/** Captures paging parameters while keeping the mapper free of null-bound predicates. */
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
