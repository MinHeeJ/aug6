package kr.ac.knue.commonfoundation.educationachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Request boundary for a faculty member's lecture-evaluation achievement draft. */
public record SaveLectureEvaluationRequest(
        @NotBlank(message = "관리항목은 필수입니다.") String managementItemCode,
        @NotNull(message = "업적발생일은 필수입니다.") LocalDate occurredOn,
        @NotBlank(message = "실적내역은 필수입니다.") String detailContent) {
}
