package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;

/** Scoped page and DB-backed inputs; empty code options intentionally prevent unsupported saves. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements,
        List<String> academicYears,
        List<String> semesters,
        List<LectureImprovementInputOption> managementItems) {
}
