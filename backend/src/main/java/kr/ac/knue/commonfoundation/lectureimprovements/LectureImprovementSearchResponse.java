package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;

/** Paged scoped data plus database choices and effective mutation capability. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements,
        List<LectureImprovementOption> academicYears,
        List<LectureImprovementOption> semesters,
        List<LectureImprovementOption> managementItems,
        boolean canCreate,
        boolean canUpdate) {
}
