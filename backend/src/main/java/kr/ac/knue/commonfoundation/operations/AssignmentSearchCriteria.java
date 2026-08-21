package kr.ac.knue.commonfoundation.operations;

import java.time.LocalDate;

public record AssignmentSearchCriteria(
        int page,
        int size,
        LocalDate referenceDate,
        String filter
) {
    public int safeSize() {
        return size == 50 || size == 100 ? size : 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }
}
