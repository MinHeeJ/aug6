package kr.ac.knue.commonfoundation.lectureimprovement;

import java.util.List;

/** Paged response for the authorized lecture-improvement list. */
public record LectureImprovementSearchResponse(
        List<LectureImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}
