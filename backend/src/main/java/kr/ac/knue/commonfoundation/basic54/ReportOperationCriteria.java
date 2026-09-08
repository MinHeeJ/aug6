package kr.ac.knue.commonfoundation.basic54;

import java.time.LocalDate;

record ReportOperationCriteria(int page, int pageSize, String reportId, String granteeType, String granteeId,
                               String requesterId, LocalDate fromDate, LocalDate toDate, String status, Long jobId,
                               LocalDate baseDate, String keyword, String evaluationYear, String organizationCode) {
    int safeSize() { return pageSize == 50 || pageSize == 100 ? pageSize : 20; }
    int offset() { return Math.max(page, 0) * safeSize(); }
}
