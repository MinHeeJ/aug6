package kr.ac.knue.commonfoundation.basic59;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveEvaluationElementManagementItemRequest(
        @NotNull(message = "규정버전을 선택하세요.") Long ruleVersionId,
        @NotBlank(message = "평가연도를 입력하세요.") String evaluationYear,
        @NotBlank(message = "평가영역 코드를 입력하세요.") String areaCode,
        @NotBlank(message = "평가요소 코드를 입력하세요.") String elementCode,
        @NotBlank(message = "관리항목 코드를 입력하세요.") String managementItemCode,
        @NotBlank(message = "관리항목명을 입력하세요.") String managementItemName,
        @NotBlank(message = "교수입력 가능부분을 입력하세요.") String teacherEditablePart,
        @NotNull(message = "정렬순서를 입력하세요.") Integer sortOrder,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}
