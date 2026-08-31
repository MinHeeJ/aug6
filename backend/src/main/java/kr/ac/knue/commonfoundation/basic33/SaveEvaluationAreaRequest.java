package kr.ac.knue.commonfoundation.basic33;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveEvaluationAreaRequest(
        @NotNull(message = "규정버전을 선택하세요.") Long ruleVersionId,
        @NotBlank(message = "평가영역 코드를 입력하세요.") String areaCode,
        @NotBlank(message = "평가영역명을 입력하세요.") String areaName,
        @NotNull(message = "정렬순서를 입력하세요.") Integer sortOrder,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "평가기간 적용방식을 입력하세요.") String periodApplyMethod,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}
