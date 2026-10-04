package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;

/** Paged lecture-improvement list response for the approved 20, 50, and 100-row options. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}
