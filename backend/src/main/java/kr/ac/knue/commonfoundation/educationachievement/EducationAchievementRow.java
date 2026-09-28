package kr.ac.knue.commonfoundation.educationachievement;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Represents a persisted education achievement returned to the list and save API clients. */
public record EducationAchievementRow(
        Long achievementId,
        String achievementType,
        String achievementStatus,
        String evaluationYear,
        Long ownerUserId,
        String managementItemCode,
        String managementItemValue,
        String achievementTitle,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate occurrenceDate,
        String evaluationConfirmedYn,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt,
        List<DegreeCompletionStudentDetail> degreeCompletionStudentDetails,
        List<StudentGuidanceDetail> studentGuidanceDetails) {
}
