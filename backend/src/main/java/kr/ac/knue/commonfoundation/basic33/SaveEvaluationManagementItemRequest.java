package kr.ac.knue.commonfoundation.basic33;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveEvaluationManagementItemRequest(
        @NotNull(message = "규정버전을 선택하세요.") Long ruleVersionId,
        @NotBlank(message = "평가영역 코드를 입력하세요.") String areaCode,
        @NotBlank(message = "평가항목 코드를 입력하세요.") String itemCode,
        @NotBlank(message = "평가연도를 입력하세요.") String evaluationYear,
        @NotBlank(message = "평가요소 코드를 입력하세요.") String elementCode,
        @NotBlank(message = "관리항목 코드를 입력하세요.") String managementItemCode,
        @NotBlank(message = "관리항목명을 입력하세요.") String managementItemName,
        @NotNull(message = "정렬순서를 입력하세요.") Integer sortOrder,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "교원 입력가능 여부를 선택하세요.") String teacherEditableYn,
        @NotBlank(message = "필수여부를 선택하세요.") String requiredYn,
        @NotBlank(message = "데이터형식을 선택하세요.") String dataType,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}
