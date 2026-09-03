package kr.ac.knue.commonfoundation.basic46;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EvaluationMaterialDeletionRequest(
        @NotBlank(message = "평가연도를 입력하세요.")
        @Pattern(regexp = "^[0-9]{4}$", message = "평가연도는 4자리 연도여야 합니다.")
        String evaluationYear,
        @NotBlank(message = "평가영역을 입력하세요.")
        String areaCode,
        @NotBlank(message = "생성배치ID를 입력하세요.")
        String generationBatchId,
        @NotBlank(message = "삭제사유를 입력하세요.")
        String deletionReason,
        @NotBlank(message = "삭제대상 미리보기 토큰을 입력하세요.")
        String previewToken) {
}
