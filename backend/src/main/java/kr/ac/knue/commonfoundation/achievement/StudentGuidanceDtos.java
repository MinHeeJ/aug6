package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Value types for individual and bulk FR-027 student-guidance operations. */
public final class StudentGuidanceDtos {
    private StudentGuidanceDtos() { }
    public record Student(@NotBlank String studentName, Map<String, Object> detail) { }
    public record SaveRequest(Long achievementId, @NotBlank String evaluationYear, @NotBlank String organizationCode,
            @NotBlank String managementItemCode, @NotNull LocalDate guidanceStartDate, @NotNull LocalDate guidanceEndDate,
            @NotEmpty List<@Valid Student> students, Map<String, Object> achievementDetail, @NotBlank String changeReason) { }
    public record Header(Long achievementId, String evaluationYear, Long ownerUserId, String organizationCode,
            String managementItemCode, LocalDate guidanceStartDate, LocalDate guidanceEndDate, int studentCount,
            String certificationStatus, LocalDateTime updatedAt) { }
    public record Row(Long achievementId, String evaluationYear, Long ownerUserId, String organizationCode,
            String managementItemCode, LocalDate guidanceStartDate, LocalDate guidanceEndDate, int studentCount,
            List<Student> students, String certificationStatus, LocalDateTime updatedAt) { }
    public record UploadResult(String uploadId, String originalFileName, int totalCount, int successCount, int errorCount,
            List<ExcelRowError> errors) { }
    public record ExcelRowError(int rowNumber, String columnName, String errorCode, String errorReason) { }
    public record CommitResult(String uploadId, int savedCount) { }
    public record UploadHistory(String uploadId, String originalFileName, Long uploaderUserId, int totalCount,
            int successCount, int errorCount, int savedCount, LocalDateTime processedAt) { }
}
