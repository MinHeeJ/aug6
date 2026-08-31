package kr.ac.knue.commonfoundation.basic33;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveEvaluationItemRequest(
        @NotNull(message = "규정버전을 선택하세요.") Long ruleVersionId,
        @NotBlank(message = "평가영역 코드를 입력하세요.") String areaCode,
        @NotBlank(message = "평가항목 코드를 입력하세요.") String itemCode,
        @NotBlank(message = "평가항목명을 입력하세요.") String itemName,
        String parentItemCode,
        @NotNull(message = "정렬순서를 입력하세요.") Integer sortOrder,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "배점 적용방식을 입력하세요.") String scoreApplyMethod,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}
