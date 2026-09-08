package kr.ac.knue.commonfoundation.basic54;

record ReportSearchCriteria(int page, int pageSize, String reportId, String businessCategory, String activeYn, String keyword, boolean includeInactive) {
    int safeSize() { return pageSize == 50 || pageSize == 100 ? pageSize : 20; }
    int offset() { return Math.max(page, 0) * safeSize(); }
}
