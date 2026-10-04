package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

/** Binds a requested employment-rate bulk action without deciding the pending OQ-83-01 policy. */
public class EmploymentRateBulkJobRequest {
    @NotBlank(message = "평가연도를 입력하세요.")
    @Pattern(regexp = "^[0-9]{4}$", message = "평가연도는 YYYY 형식이어야 합니다.")
    private String evaluationYear;

    @NotBlank(message = "처리유형을 입력하세요.")
    @Pattern(regexp = "^(GENERATE|DELETE)$", message = "처리유형은 GENERATE 또는 DELETE여야 합니다.")
    private String actionType;

    private Map<String, Object> targetCondition;

    public String getEvaluationYear() {
        return evaluationYear;
    }

    public void setEvaluationYear(String evaluationYear) {
        this.evaluationYear = evaluationYear;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Map<String, Object> getTargetCondition() {
        return targetCondition;
    }

    public void setTargetCondition(Map<String, Object> targetCondition) {
        this.targetCondition = targetCondition;
    }
}
