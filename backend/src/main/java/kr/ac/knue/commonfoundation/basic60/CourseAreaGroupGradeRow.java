package kr.ac.knue.commonfoundation.basic60;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseAreaGroupGradeRow(
        Long resultId,
        Long facultyUserId,
        String employeeNo,
        String facultyName,
        String completionType,
        String semester,
        String courseArea,
        BigDecimal groupGrade,
        String detailSummary,
        String publishedYn,
        LocalDateTime evaluatedAt) {}
