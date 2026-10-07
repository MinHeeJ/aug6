package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Materializes the common header and its one-to-one improvement detail without changing identity. */
public record EmploymentRateImprovementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String certificationStatus,
        @JsonIgnore String attachmentRef,
        LocalDate specialLectureStartDate,
        LocalDate specialLectureEndDate,
        String mockExamQuestionPeriod,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    /** Tokens are serialized as the API array, never as a local file path. */
    @JsonProperty("attachmentIds")
    public List<String> attachmentIds() {
        if (attachmentRef == null) {
            return java.util.Collections.emptyList();
        }
        try {
            return new ObjectMapper().readValue(attachmentRef, new TypeReference<List<String>>() { });
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("저장된 첨부 참조 형식 오류", exception);
        }
    }
}
