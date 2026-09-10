package kr.ac.knue.commonfoundation.basic59;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseAreaGroupGradeRow(
        Long gradeId,
        String evaluationYear,
        Long teacherUserId,
        String teacherName,
        String collegeCode,
        String departmentCode,
        String completionTypeCode,
        String semesterCode,
        String courseAreaCode,
        String courseAreaName,
        String groupGrade,
        BigDecimal totalScore,
        String publishedYn,
        String finalizationStatus,
        LocalDateTime updatedAt) {
}
