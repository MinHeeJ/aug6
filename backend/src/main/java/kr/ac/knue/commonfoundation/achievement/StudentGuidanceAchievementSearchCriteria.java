package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/** Optional student-guidance list filters with the authenticated caller's data-scope context. */
public record StudentGuidanceAchievementSearchCriteria(
        int page, int size, String managementNo, String teacherName, String managementItemCode,
        LocalDate guidanceDateFrom, LocalDate guidanceDateTo, String certificationStatus,
        Long viewerUserId, String viewerRole
) {
    public int safePage() { return Math.max(page, 0); }
    public int safeSize() { return size == 50 || size == 100 ? size : 20; }
    public int offset() { return safePage() * safeSize(); }
}
