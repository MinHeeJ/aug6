package kr.ac.knue.commonfoundation.basic46;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EvaluationMaterialGenerationRequest(
        @NotBlank(message = "평가연도를 입력하세요.")
        @Pattern(regexp = "^[0-9]{4}$", message = "평가연도는 4자리 연도여야 합니다.")
        String evaluationYear,
        @NotBlank(message = "평가영역을 입력하세요.")
        String areaCode,
        String organizationCode,
        Long targetUserId,
        @NotBlank(message = "생성 사유를 입력하세요.")
        String reason) {
}
