package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** API representation of one caller-visible 취업률 제고 실적 and its detail fields. */
public record EmploymentRateImprovementAchievementResponse(
        Long achievementId,
        String teacherLoginId,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementStatus,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        String mockExamQuestionPeriod,
        List<String> attachmentIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
