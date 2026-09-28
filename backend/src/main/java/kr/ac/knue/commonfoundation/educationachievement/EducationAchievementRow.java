package kr.ac.knue.commonfoundation.educationachievement;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents a persisted education-area achievement returned to portal list and detail flows.
 */
public record EducationAchievementRow(
        Long achievementId,
        Long facultyUserId,
        String achievementType,
        String managementItemCode,
        LocalDate occurrenceDate,
        String certificationStatus,
        String attachmentToken,
        List<DegreeCompletionDetail> degreeCompletionDetails,
        List<StudentGuidanceDetail> studentGuidanceDetails) {

    /** MyBatis constructor used by common-master list queries that do not join detail rows. */
    public EducationAchievementRow(Long achievementId, Long facultyUserId, String achievementType,
            String managementItemCode, LocalDate occurrenceDate, String certificationStatus, String attachmentToken) {
        this(achievementId, facultyUserId, achievementType, managementItemCode, occurrenceDate,
                certificationStatus, attachmentToken, List.of(), List.of());
    }

    /** Compatibility constructor for callers that only supply degree-completion details. */
    public EducationAchievementRow(Long achievementId, Long facultyUserId, String achievementType,
            String managementItemCode, LocalDate occurrenceDate, String certificationStatus, String attachmentToken,
            List<DegreeCompletionDetail> degreeCompletionDetails) {
        this(achievementId, facultyUserId, achievementType, managementItemCode, occurrenceDate,
                certificationStatus, attachmentToken, degreeCompletionDetails, List.of());
    }

    /** Adds degree details after the master row has been persisted or rehydrated. */
    public EducationAchievementRow withDegreeCompletionDetails(List<DegreeCompletionDetail> details) {
        return new EducationAchievementRow(achievementId, facultyUserId, achievementType, managementItemCode,
                occurrenceDate, certificationStatus, attachmentToken, details, studentGuidanceDetails);
    }

    /** Adds student-guidance details after the master row has been persisted or rehydrated. */
    public EducationAchievementRow withStudentGuidanceDetails(List<StudentGuidanceDetail> details) {
        return new EducationAchievementRow(achievementId, facultyUserId, achievementType, managementItemCode,
                occurrenceDate, certificationStatus, attachmentToken, degreeCompletionDetails, details);
    }
}
