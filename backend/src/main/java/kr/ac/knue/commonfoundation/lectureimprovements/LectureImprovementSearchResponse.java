package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;

/** Scoped rows and count share the exact same SQL predicates. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementRow> achievements, int page, int pageSize, long totalElements) {
}
