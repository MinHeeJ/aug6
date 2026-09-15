package kr.ac.knue.commonfoundation.basic65;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

record StudentGuidanceAchievementRow(Long achievementId, Long facultyUserId, String evaluationYear, String academicYear, String semester,
        String studentNo, String studentName, String guidanceType, LocalDate guidanceDate, String guidanceContent, String achievementStatus,
        Long managementItemSettingId, String dynamicFieldsJson, String attachmentRefsJson, LocalDateTime createdAt, LocalDateTime updatedAt) { }
record StudentGuidanceAchievementSearchCriteria(int page, int size, String evaluationYear, String academicYear, String semester, String studentKeyword, String guidanceType, String achievementStatus) {
    int safePage() { return Math.max(0, page); }
    int safeSize() { return size == 20 || size == 50 || size == 100 ? size : 20; }
}
record StudentGuidanceAchievementSearchResponse(List<StudentGuidanceAchievementRow> studentGuidanceAchievements, int page, int size, long totalElements) { }
record SaveStudentGuidanceAchievementRequest(Long achievementId, @NotBlank String evaluationYear, @NotBlank String academicYear, @NotBlank String semester,
        @NotBlank String studentNo, @NotBlank String studentName, @NotBlank String guidanceType, @NotNull LocalDate guidanceDate,
        @NotBlank String guidanceContent, Long managementItemSettingId, Map<String, String> dynamicFields, List<String> attachmentRefs, @NotBlank String changeReason) { }
record StudentGuidanceExcelRowError(int rowNumber, String columnName, String errorCode, String errorReason) { }
record StudentGuidanceExcelUploadResult(String uploadId, String validationStatus, int totalCount, int successCount, int errorCount, int savedCount, List<StudentGuidanceExcelRowError> errors) { }
