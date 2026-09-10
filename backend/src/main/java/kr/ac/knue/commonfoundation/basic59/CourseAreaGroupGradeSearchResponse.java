package kr.ac.knue.commonfoundation.basic59;

import java.util.List;

public record CourseAreaGroupGradeSearchResponse(
        List<CourseAreaGroupGradeRow> courseAreaGroupGrades,
        int page,
        int pageSize,
        long totalElements) {
}
