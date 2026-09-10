package kr.ac.knue.commonfoundation.basic60;

public record CourseAreaGroupGradeSearchCriteria(
        int page,
        int pageSize,
        String completionType,
        String semester,
        String courseArea,
        Long facultyUserId,
        String keyword) {
    public int safeSize() {
        if (pageSize == 50 || pageSize == 100) return pageSize;
        return 20;
    }
    public int offset() { return Math.max(page, 0) * safeSize(); }
    public String normalizedCompletionType() { return upperOrNull(completionType); }
    public String normalizedSemester() { return semester == null || semester.isBlank() ? null : semester.trim(); }
    public String normalizedCourseArea() { return upperOrNull(courseArea); }
    public String normalizedKeyword() { return keyword == null || keyword.isBlank() ? null : keyword.trim(); }
    private String upperOrNull(String value) { return value == null || value.isBlank() ? null : value.trim().toUpperCase(); }
}
