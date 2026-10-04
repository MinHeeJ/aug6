package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;

/** Supplies the paged lecture-improvement list expected by the faculty screen. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}
