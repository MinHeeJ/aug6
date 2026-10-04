package kr.ac.knue.commonfoundation.lectureimprovements;

/** Returns the read-back row and the non-blocking evaluation-period warning after a save. */
public record LectureImprovementSaveResponse(
        LectureImprovementRow achievement,
        boolean achievementDateWarning,
        String warningMessage) {
}
