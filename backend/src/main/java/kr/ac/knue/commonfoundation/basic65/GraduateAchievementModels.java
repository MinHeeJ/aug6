package kr.ac.knue.commonfoundation.basic65;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

record GraduateAchievementRow(Long achievementId, Long facultyUserId, String evaluationYear, String academicYear, String semester,
        String studentNo, String studentName, String degreeType, String thesisTitle, LocalDate awardDate, String achievementStatus,
        Long managementItemSettingId, String dynamicFieldsJson, String attachmentRefsJson, LocalDateTime createdAt, LocalDateTime updatedAt) { }

record GraduateAchievementSearchCriteria(int page, int size, String evaluationYear, String academicYear, String semester,
        String studentKeyword, String degreeType, String achievementStatus) {
    int safePage() { return Math.max(0, page); }
    int safeSize() { return size == 20 || size == 50 || size == 100 ? size : 20; }
}

record GraduateAchievementSearchResponse(List<GraduateAchievementRow> graduateAchievements, int page, int size, long totalElements) { }

record SaveGraduateAchievementRequest(Long achievementId, @NotBlank String evaluationYear, @NotBlank String academicYear,
        @NotBlank String semester, @NotBlank String studentNo, @NotBlank String studentName, @NotBlank String degreeType,
        @NotBlank String thesisTitle, @NotNull LocalDate awardDate, Long managementItemSettingId,
        Map<String, String> dynamicFields, List<String> attachmentRefs, @NotBlank String changeReason) { }
