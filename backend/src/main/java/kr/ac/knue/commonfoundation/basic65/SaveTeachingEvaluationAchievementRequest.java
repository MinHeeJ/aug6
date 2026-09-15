package kr.ac.knue.commonfoundation.basic65;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SaveTeachingEvaluationAchievementRequest(
        @NotBlank(message = "평가연도를 입력하세요.") String evaluationYear,
        @NotBlank(message = "학사연도를 입력하세요.") String academicYear,
        @NotBlank(message = "학기를 입력하세요.") String semester,
        @NotBlank(message = "강좌코드를 입력하세요.") String courseCode,
        @NotBlank(message = "강좌명을 입력하세요.") String courseName,
        @NotNull(message = "강의평가 점수를 입력하세요.")
        @DecimalMin(value = "0.0", message = "강의평가 점수는 0 이상이어야 합니다.")
        @DecimalMax(value = "100.0", message = "강의평가 점수는 100 이하여야 합니다.") BigDecimal evaluationScore,
        Long managementItemSettingId,
        Map<String, String> dynamicFields,
        List<String> attachmentRefs,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}
