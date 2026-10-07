package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

/** Header/detail projection with the approved integer semester response. */
public record LectureImprovementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementContent,
        Integer academicYear,
        Integer semester,
        String achievementStatus,
        @JsonIgnore String attachmentRef) {
    @JsonProperty("attachmentIds")
    public List<String> attachmentIds() {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    attachmentRef == null ? "[]" : attachmentRef,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Invalid stored attachment references", exception);
        }
    }
}
