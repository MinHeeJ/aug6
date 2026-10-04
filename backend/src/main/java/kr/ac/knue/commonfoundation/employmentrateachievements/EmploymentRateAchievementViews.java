package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Stable API views for the employment-rate achievement list, detail, upload, and queued-job surfaces. */
public final class EmploymentRateAchievementViews {
    private EmploymentRateAchievementViews() {
    }

    public record Achievement(
            Long achievementId,
            String managementNo,
            Long teacherUserId,
            String teacherName,
            String evaluationYear,
            String managementItemCode,
            LocalDate achievementDate,
            String achievementName,
            List<String> attachmentIds,
            String achievementStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record SearchResponse(List<Achievement> achievements, int page, int pageSize, long totalElements) {
    }

    public record UploadResult(
            String uploadId,
            String originalFileName,
            int totalCount,
            int successCount,
            int errorCount,
            List<UploadError> errors) {
    }

    public record UploadError(
            int rowNumber,
            String columnName,
            String inputValue,
            String errorCode,
            String errorReason) {
    }

    public record BulkJob(
            String jobId,
            String evaluationYear,
            String actionType,
            String jobStatus,
            int totalCount,
            int processedCount,
            int successCount,
            int failureCount,
            LocalDateTime requestedAt,
            LocalDateTime completedAt) {
    }
}
