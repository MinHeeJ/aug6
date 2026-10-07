package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;

/** Header/detail projection. Academic year remains independent of the immutable evaluation year. */
public record LectureImprovementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementStatus,
        String achievementContent,
        Integer academicYear,
        Integer semester,
        @JsonIgnore String attachmentRef) {
    @JsonProperty("attachmentIds")
    public List<String> attachmentIds() {
        try {
            return new ObjectMapper().readValue(attachmentRef, new TypeReference<List<String>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid persisted attachment references", exception);
        }
    }
}
