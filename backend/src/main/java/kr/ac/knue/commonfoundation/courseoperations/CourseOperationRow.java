package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Materializes the common header and course detail in explicit SQL constructor order. */
public record CourseOperationRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String performanceDetails,
        String achievementStatus,
        @JsonIgnore String attachmentRef,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    /** Exposes opaque references as the approved array, never as a storage path. */
    @JsonProperty("attachmentIds")
    public List<String> attachmentIds() {
        try {
            return new ObjectMapper().readValue(attachmentRef == null ? "[]" : attachmentRef,
                    new TypeReference<List<String>>() { });
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Invalid stored attachment references", exception);
        }
    }
}
