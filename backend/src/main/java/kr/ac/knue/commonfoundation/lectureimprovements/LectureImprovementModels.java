package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Transport and persistence projections for the BASIC-83 lecture-improvement
 * API. The models keep the common achievement header separate from its detail.
 */
public final class LectureImprovementModels {
    private LectureImprovementModels() {
    }

    /** Validated create or update payload mandated by the approved OpenAPI operation. */
    public record Request(
            @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
            @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
            @NotBlank(message = "강의개선 내용을 입력하세요.") String achievementContent,
            @NotNull(message = "학년도를 입력하세요.")
            @Min(value = 2000, message = "학년도는 2000 이상이어야 합니다.") Integer academicYear,
            @NotNull(message = "학기를 입력하세요.") Integer semester,
            List<String> attachmentIds) {
    }

    /** Caller-supplied paging values normalized before binding to the mapper. */
    public record SearchCriteria(int page, int pageSize) {
        public int safePage() {
            return Math.max(page, 0);
        }

        public int safePageSize() {
            return pageSize;
        }

        public int offset() {
            return safePage() * safePageSize();
        }
    }

    /**
     * Mutable command object used exclusively for PostgreSQL generated-key
     * materialization. MyBatis writes {@code achievementId} after the header
     * insert so the detail and lifecycle records share the database identity.
     */
    public static final class CreateCommand {
        private final Long teacherUserId;
        private final String organizationCode;
        private final String evaluationYear;
        private final String managementItemCode;
        private final LocalDate achievementDate;
        private final String achievementName;
        private final String attachmentIds;
        private final Long createdBy;
        private Long achievementId;

        public CreateCommand(
                Long teacherUserId,
                String organizationCode,
                String evaluationYear,
                String managementItemCode,
                LocalDate achievementDate,
                String achievementName,
                String attachmentIds,
                Long createdBy) {
            this.teacherUserId = teacherUserId;
            this.organizationCode = organizationCode;
            this.evaluationYear = evaluationYear;
            this.managementItemCode = managementItemCode;
            this.achievementDate = achievementDate;
            this.achievementName = achievementName;
            this.attachmentIds = attachmentIds;
            this.createdBy = createdBy;
        }

        public Long getTeacherUserId() {
            return teacherUserId;
        }

        public String getOrganizationCode() {
            return organizationCode;
        }

        public String getEvaluationYear() {
            return evaluationYear;
        }

        public String getManagementItemCode() {
            return managementItemCode;
        }

        public LocalDate getAchievementDate() {
            return achievementDate;
        }

        public String getAchievementName() {
            return achievementName;
        }

        public String getAttachmentIds() {
            return attachmentIds;
        }

        public Long getCreatedBy() {
            return createdBy;
        }

        public Long getAchievementId() {
            return achievementId;
        }

        public void setAchievementId(Long achievementId) {
            this.achievementId = achievementId;
        }
    }

    /** Materialized common header and lecture-improvement detail for list and detail responses. */
    public record Row(
            Long achievementId,
            Long teacherUserId,
            String teacherName,
            String organizationCode,
            String evaluationYear,
            String managementItemCode,
            LocalDate achievementDate,
            String achievementName,
            String achievementStatus,
            String attachmentIds,
            String achievementContent,
            Integer academicYear,
            Integer semester,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    /** Stable paged list envelope payload used by the list operation. */
    public record SearchResponse(
            List<Row> achievements,
            int page,
            int pageSize,
            long totalElements) {
    }

    /** Save payload including the non-blocking evaluation-period warning. */
    public record SaveResult(Row achievement, boolean occurredDateWarning, String warningMessage) {
    }
}
