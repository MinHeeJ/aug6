package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * Carries the approved lecture-improvement fields from the HTTP boundary into
 * the transactional application service.
 */
public record LectureImprovementRequest(
        @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
        @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
        @NotBlank(message = "실적내용을 입력하세요.") String achievementContent,
        @NotNull(message = "학년도를 입력하세요.") @Min(value = 2000, message = "학년도는 2000년 이상이어야 합니다.") Integer academicYear,
        @NotNull(message = "학기를 입력하세요.") @Min(value = 1, message = "학기는 1 또는 2여야 합니다.")
        @Max(value = 2, message = "학기는 1 또는 2여야 합니다.") Integer semester,
        List<String> attachmentIds) {
}
