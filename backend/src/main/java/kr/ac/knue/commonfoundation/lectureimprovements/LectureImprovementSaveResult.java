package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.Map;

/** Saved detail and non-blocking evaluation-date warning. */
public record LectureImprovementSaveResult(
        Map<String, Object> achievement, boolean occurredDateWarning, String warningMessage) {
}
