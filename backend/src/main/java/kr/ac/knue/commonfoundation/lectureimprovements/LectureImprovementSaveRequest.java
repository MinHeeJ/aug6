package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

/** Defines the approved create and update payload for a lecture-improvement achievement. */
public record LectureImprovementSaveRequest(
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        @NotBlank(message = "강의개선 내용을 입력하세요.") String achievementContent,
        @NotNull(message = "학년도를 입력하세요.") @Min(value = 2000, message = "학년도는 2000 이상이어야 합니다.") Integer academicYear,
        @NotNull(message = "학기를 입력하세요.") @Pattern(regexp = "[12]", message = "학기는 1 또는 2여야 합니다.") String semester,
        List<String> attachmentIds) {
    /** Converts the OpenAPI semester representation to the persisted integer code. */
    public Integer semesterCode() {
        return semester == null ? null : Integer.valueOf(semester);
    }
}
