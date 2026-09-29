package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.List;

/** Read model that keeps degree-completion header fields and its supervised students together. */
public record DegreeCompletionAchievementRow(Long achievementId, String managementNo, String evaluationYear,
        Long teacherUserId, String teacherName, String managementItemCode, LocalDate occurredDate,
        String certificationStatus, boolean hasAttachment, List<DegreeCompletionStudent> students) {
}
