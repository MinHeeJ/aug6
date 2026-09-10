package kr.ac.knue.commonfoundation.basic59;

public record CourseAreaGroupGradeSearchCriteria(
        int page,
        int pageSize,
        Long teacherUserId,
        String completionTypeCode,
        String semesterCode,
        String courseAreaCode) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safeSize() {
        return pageSize == 50 || pageSize == 100 ? pageSize : 20;
    }

    public int offset() {
        return safePage() * safeSize();
    }

    public Long effectiveTeacherUserId() {
        return teacherUserId;
    }

    public CourseAreaGroupGradeSearchCriteria withTeacherUserId(Long effectiveTeacherUserId) {
        return new CourseAreaGroupGradeSearchCriteria(page, pageSize, effectiveTeacherUserId, completionTypeCode, semesterCode, courseAreaCode);
    }

    public String normalizedCompletionTypeCode() {
        return normalize(completionTypeCode);
    }

    public String normalizedSemesterCode() {
        return normalize(semesterCode);
    }

    public String normalizedCourseAreaCode() {
        return normalize(courseAreaCode);
    }

    private String normalize(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isBlank() ? null : trimmed.toUpperCase();
    }
}
