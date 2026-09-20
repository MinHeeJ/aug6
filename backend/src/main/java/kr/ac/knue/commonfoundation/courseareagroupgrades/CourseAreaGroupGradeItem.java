package kr.ac.knue.commonfoundation.courseareagroupgrades;

import java.math.BigDecimal;

/**
 * Read-only result item exposed by the course-area group grade query API.
 */
public record CourseAreaGroupGradeItem(
        Long resultId,
        Long facultyUserId,
        String employeeNo,
        String facultyName,
        String completionType,
        String semester,
        String courseArea,
        BigDecimal groupGrade,
        String detailSummary) {}
