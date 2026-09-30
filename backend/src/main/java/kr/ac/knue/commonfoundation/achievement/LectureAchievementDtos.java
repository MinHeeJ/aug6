package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * HTTP and persistence-facing value types for the BASIC-79 lecture achievement flow.
 */
public final class LectureAchievementDtos {
    private LectureAchievementDtos() { }

    public record SearchCriteria(int page, int size, String evaluationYear, String organizationCode,
                                 String certificationStatus, String managementItemCode) {
        public int safePage() { return Math.max(page, 0); }
        public int safeSize() { return size == 20 || size == 50 || size == 100 ? size : 20; }
        public int offset() { return safePage() * safeSize(); }
    }

    public record Row(Long achievementId, String evaluationYear, Long ownerUserId, String organizationCode,
                      String managementItemCode, LocalDate occurredDate, Map<String, Object> achievementDetail,
                      String certificationStatus, String attachmentReference, LocalDateTime updatedAt) { }

    public record SearchResponse(List<Row> achievements, int page, int size, long totalElements) { }

    public record SaveRequest(Long achievementId, @NotBlank String evaluationYear, @NotBlank String organizationCode,
                              @NotBlank String managementItemCode, @NotNull LocalDate occurredDate,
                              Map<String, Object> achievementDetail, String attachmentReference,
                              @NotBlank String changeReason) { }

    public record SaveResponse(Row achievement, List<String> warnings) { }

    public record AttachmentReferenceRequest(@NotBlank String attachmentReference, @NotBlank String changeReason) { }
}
