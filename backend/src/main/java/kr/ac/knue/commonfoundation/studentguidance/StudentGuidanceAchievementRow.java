package kr.ac.knue.commonfoundation.studentguidance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read model for a student-guidance achievement and its persisted guided students. */
public record StudentGuidanceAchievementRow(Long achievementId, String managementNo, String teacherName,
        String managementItemCode, String organizationCode, LocalDate occurredDate, LocalDate guidanceStartDate,
        LocalDate guidanceEndDate, Integer studentCount, String achievementDetail, String certificationStatus,
        String attachmentRef, LocalDateTime updatedAt, Long updatedBy, List<StudentGuidanceStudentRequest> students) {
    public StudentGuidanceAchievementRow(Long achievementId, String managementNo, String teacherName,
            String managementItemCode, String organizationCode, LocalDate occurredDate, LocalDate guidanceStartDate,
            LocalDate guidanceEndDate, Integer studentCount, String achievementDetail, String certificationStatus,
            String attachmentRef, LocalDateTime updatedAt, Long updatedBy) {
        this(achievementId, managementNo, teacherName, managementItemCode, organizationCode, occurredDate,
                guidanceStartDate, guidanceEndDate, studentCount, achievementDetail, certificationStatus,
                attachmentRef, updatedAt, updatedBy, List.of());
    }
}
