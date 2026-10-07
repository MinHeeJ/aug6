package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;

/** Scoped list plus server-owned management-item metadata, including empty lists. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementRow> achievements, int page, int pageSize, long totalElements,
        List<LectureImprovementManagementItem> managementItems) {
}
