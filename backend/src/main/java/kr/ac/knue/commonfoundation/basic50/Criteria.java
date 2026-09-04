package kr.ac.knue.commonfoundation.basic50;

record BusinessSettingCriteria(int page, int pageSize, String evaluationYear, String organizationCode, String evaluationUnitCode,
                               String activeYn, String keyword, Long requesterUserId, boolean restrictOrganizationScope) {
    int safeSize() { return pageSize == 50 || pageSize == 100 ? pageSize : 20; }
    int offset() { return Math.max(page, 0) * safeSize(); }
}

record ResearchCriterionCriteria(int page, int pageSize, String areaCode, String managementCriterionCode, String activeYn, String keyword) {
    int safeSize() { return pageSize == 50 || pageSize == 100 ? pageSize : 20; }
    int offset() { return Math.max(page, 0) * safeSize(); }
}

record ResearchAchievementCriteria(int page, int pageSize, String evaluationYear, String organizationCode, String areaCode,
                                   String confirmationStatus, Long teacherUserId, String keyword) {
    int safeSize() { return pageSize == 50 || pageSize == 100 ? pageSize : 20; }
    int offset() { return Math.max(page, 0) * safeSize(); }
}
