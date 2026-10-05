package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Normalizes list paging while keeping optional query predicates dynamic in MyBatis. */
public record EmploymentRateImprovementSearchCriteria(int page, int pageSize) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safePageSize() {
        return pageSize == 20 || pageSize == 50 || pageSize == 100 ? pageSize : 20;
    }

    public int offset() {
        return safePage() * safePageSize();
    }
}
