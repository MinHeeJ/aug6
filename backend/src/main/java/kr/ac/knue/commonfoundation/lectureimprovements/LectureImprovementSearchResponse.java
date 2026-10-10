package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;
import java.util.Map;

/** Scoped results and database-driven form options share the authorized list operation. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementRow> achievements, int page, int pageSize, long totalElements,
        List<Map<String, Object>> semesters, List<Map<String, Object>> managementItems) {
}
