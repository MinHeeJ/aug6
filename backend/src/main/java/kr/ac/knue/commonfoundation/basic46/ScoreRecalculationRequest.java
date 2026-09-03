package kr.ac.knue.commonfoundation.basic46;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ScoreRecalculationRequest(
        @NotBlank(message = "평가연도를 입력하세요.")
        @Pattern(regexp = "^[0-9]{4}$", message = "평가연도는 4자리 연도여야 합니다.")
        String evaluationYear,
        @NotBlank(message = "평가영역을 입력하세요.")
        String areaCode,
        Long targetUserId,
        @NotBlank(message = "산식버전을 입력하세요.")
        String formulaVersionId,
        @NotBlank(message = "선택 사유를 입력하세요.")
        String selectionReason) {
}
