package kr.ac.knue.commonfoundation.lectureimprovement;

/** Captures list pagination without binding optional null predicates into SQL. */
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
