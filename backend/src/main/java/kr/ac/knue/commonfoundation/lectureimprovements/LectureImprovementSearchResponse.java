package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;

/** Paged response that preserves the approved 20, 50, and 100-row list contract. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementAchievement> achievements,
        int page,
        int pageSize,
        long totalElements) {
}
