package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Captures list pagination without binding absent filters into PostgreSQL predicates. */
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
