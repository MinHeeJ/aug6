package kr.ac.knue.commonfoundation.basic71;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;

/** Read model returned by the education-achievement faculty APIs. */
public record EducationAchievementRow(
        Long achievementId,
        String achievementType,
        Long teacherUserId,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementOccurredOn,
        String certificationStatus,
        boolean deleted,
        String details,
        List<String> attachmentFileTokens,
        List<GraduateDegreeCompletionStudentRow> degreeCompletionStudents) {

    /** MyBatis projects scalar achievement columns first; service hydration loads child collections. */
    EducationAchievementRow(Long achievementId, String achievementType, Long teacherUserId,
            String evaluationYear, String managementItemCode, LocalDate achievementOccurredOn,
            String certificationStatus, boolean deleted, String details) {
        this(achievementId, achievementType, teacherUserId, evaluationYear, managementItemCode,
                achievementOccurredOn, certificationStatus, deleted, details, List.of(), List.of());
    }

    EducationAchievementRow withChildren(List<String> attachments, List<GraduateDegreeCompletionStudentRow> students) {
        return new EducationAchievementRow(achievementId, achievementType, teacherUserId, evaluationYear,
                managementItemCode, achievementOccurredOn, certificationStatus, deleted, details, attachments, students);
    }
}

record EducationAchievementSearchCriteria(int page, int size, String achievementType) {
    int safePage() { return Math.max(page, 0); }
    int safeSize() { return size == 50 || size == 100 ? size : 20; }
    int offset() { return safePage() * safeSize(); }
    String normalizedAchievementType() { return achievementType == null || achievementType.isBlank() ? null : achievementType.trim().toUpperCase(); }
}

record EducationAchievementSearchResponse(List<EducationAchievementRow> achievements, int page, int size, long totalElements) {}

record SaveEducationAchievementRequest(
        String achievementType,
        String managementItemCode,
        LocalDate achievementOccurredOn,
        JsonNode details,
        List<String> attachmentFileTokens,
        List<GraduateDegreeCompletionStudentRequest> degreeCompletionStudents) {}

record GraduateDegreeCompletionStudentRequest(String degreeTypeCode, String studentName, String thesisTitle, LocalDate degreeAwardedOn) {}
record GraduateDegreeCompletionStudentRow(Long degreeCompletionStudentId, Long achievementId, String degreeTypeCode, String studentName, String thesisTitle, LocalDate degreeAwardedOn, int sortOrder) {}
record EducationAchievementTransitionRequest(String actionType, String reasonCode, String opinion, String evidenceRef) {}
record StudentGuidanceUploadError(int rowNumber, String columnName, String errorReason) {}
record StudentGuidanceUploadResult(String uploadId, int totalCount, int normalCount, int errorCount, List<StudentGuidanceUploadError> errors) {}
