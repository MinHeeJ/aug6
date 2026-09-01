package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDate;

public record KorusFacultySyncSearchCriteria(int page, int pageSize, LocalDate targetStartDate,
                                             LocalDate targetEndDate, String syncStatus, String requestId,
                                             String employeeNo) {
    public int safeSize() {
        return pageSize == 50 || pageSize == 100 ? pageSize : 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedSyncStatus() {
        return normalize(syncStatus);
    }

    public String normalizedRequestId() {
        return hasText(requestId) ? requestId.trim() : null;
    }

    public String normalizedEmployeeNo() {
        return normalize(employeeNo);
    }

    private String normalize(String value) {
        return hasText(value) ? value.trim().toUpperCase() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
