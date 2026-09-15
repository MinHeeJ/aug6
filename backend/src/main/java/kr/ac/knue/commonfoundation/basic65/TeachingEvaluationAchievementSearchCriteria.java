package kr.ac.knue.commonfoundation.basic65;

public record TeachingEvaluationAchievementSearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String academicYear,
        String semester,
        String courseKeyword,
        String evaluationStatus) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safeSize() {
        return size == 50 || size == 100 ? size : 20;
    }
}
