package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;

/** Caller-scoped rows and the count computed with the same predicate. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementRow> achievements, int page, int pageSize, long totalElements) {
}
