package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Captures list paging while keeping optional SQL predicates out of null bindings. */
public record EmploymentRateImprovementSearchCriteria(int page, int pageSize) {
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
