package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;
import java.util.Map;

/** Scoped persistent records and matching total, with DB-backed form choices. */
public record LectureImprovementSearchResponse(
        List<Map<String, Object>> achievements, int page, int pageSize,
        long totalElements, List<Map<String, Object>> managementItems) {
}
