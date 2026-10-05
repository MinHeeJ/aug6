package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;

/** Provides the paged lecture-improvement list required by the faculty screen. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}
