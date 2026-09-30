package kr.ac.knue.commonfoundation.achievement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Value types for FR-028 headers and guided-student details. */
public final class DegreeCompletionDtos {
    private DegreeCompletionDtos() { }
    public record SearchCriteria(int page, int size, String evaluationYear, String organizationCode, String certificationStatus, String managementItemCode) {
        public int safePage() { return Math.max(page, 0); }
        public int safeSize() { return size == 20 || size == 50 || size == 100 ? size : 20; }
        public int offset() { return safePage() * safeSize(); }
    }
    public record Student(@NotBlank String degreeType, @NotBlank String studentName, @NotBlank String thesisTitle, @NotNull LocalDate degreeAwardedDate) { }
    public record Header(Long achievementId, String evaluationYear, Long ownerUserId, String organizationCode, String managementItemCode, LocalDate occurredDate, Map<String,Object> achievementDetail, String certificationStatus, String attachmentReference, LocalDateTime updatedAt) { }
    public record Row(Long achievementId, String evaluationYear, Long ownerUserId, String organizationCode, String managementItemCode, LocalDate occurredDate, Map<String,Object> achievementDetail, String certificationStatus, String attachmentReference, List<Student> students, LocalDateTime updatedAt) { }
    public record SearchResponse(List<Row> achievements, int page, int size, long totalElements) { }
    public record SaveRequest(Long achievementId, @NotBlank String evaluationYear, @NotBlank String organizationCode, @NotBlank String managementItemCode, @NotNull LocalDate occurredDate, Map<String,Object> achievementDetail, String attachmentReference, @NotEmpty List<@Valid Student> students, @NotBlank String changeReason) { }
}
