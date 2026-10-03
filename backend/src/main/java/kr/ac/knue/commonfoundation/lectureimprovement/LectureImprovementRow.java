package kr.ac.knue.commonfoundation.lectureimprovement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read model for a lecture-improvement achievement returned by list and detail routes. */
public record LectureImprovementRow(
        Long achievementId,
        String managementNo,
        Long targetUserId,
        String teacherName,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementContent,
        Integer academicYear,
        Integer semester,
        String certificationStatus,
        List<String> attachmentIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
