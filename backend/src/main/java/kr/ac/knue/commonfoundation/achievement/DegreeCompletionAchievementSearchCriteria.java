package kr.ac.knue.commonfoundation.achievement;

/** Normalized optional filters and bounded pagination for degree-completion achievement searches. */
public record DegreeCompletionAchievementSearchCriteria(
        int page,
        int size,
        String managementNo,
        String teacherName,
        String certificationStatus,
        Long viewerUserId,
        String viewerRole
) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safeSize() {
        return size == 50 || size == 100 ? size : 20;
    }

    public int offset() {
        return safePage() * safeSize();
    }
}
