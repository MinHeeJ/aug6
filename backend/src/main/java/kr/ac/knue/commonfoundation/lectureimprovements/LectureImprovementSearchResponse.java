package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;

/** Scoped list and its total use the identical persistence predicate. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementRow> achievements, int page, int pageSize, long totalElements) {
}
