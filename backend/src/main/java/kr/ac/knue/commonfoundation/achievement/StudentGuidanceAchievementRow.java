package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read model that keeps guidance header data and its persisted student details together. */
public record StudentGuidanceAchievementRow(
        Long achievementId, String managementNo, Long teacherUserId, String teacherName,
        String evaluationYear, String organizationCode, String managementItemCode,
        LocalDate guidanceStartDate, LocalDate guidanceEndDate, String certificationStatus,
        boolean attachmentPresent, String students,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
}
