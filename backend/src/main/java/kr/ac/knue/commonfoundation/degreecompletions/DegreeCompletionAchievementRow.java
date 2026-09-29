package kr.ac.knue.commonfoundation.degreecompletions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read model for a degree-completion achievement and its persisted student details. */
public record DegreeCompletionAchievementRow(Long achievementId, String managementNo, String teacherName,
        String managementItemCode, String organizationCode, LocalDate occurredDate, String achievementDetail,
        String certificationStatus, String attachmentRef, LocalDateTime updatedAt, Long updatedBy,
        List<DegreeCompletionStudentRequest> students) {
    public DegreeCompletionAchievementRow(Long achievementId, String managementNo, String teacherName,
            String managementItemCode, String organizationCode, LocalDate occurredDate, String achievementDetail,
            String certificationStatus, String attachmentRef, LocalDateTime updatedAt, Long updatedBy) {
        this(achievementId, managementNo, teacherName, managementItemCode, organizationCode, occurredDate,
                achievementDetail, certificationStatus, attachmentRef, updatedAt, updatedBy, List.of());
    }
}
