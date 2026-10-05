package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;

/** Provides the paged lecture-improvement list response required by the API contract. */
public record LectureImprovementListResponse(
        List<LectureImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}
