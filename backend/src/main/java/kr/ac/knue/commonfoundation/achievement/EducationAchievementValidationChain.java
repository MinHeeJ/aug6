package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;

/**
 * Applies the common authorization, data-scope, period, confirmed-data lock,
 * occurrence-date warning, and transition-input rules before an education
 * achievement write reaches its feature-specific persistence service.
 */
public final class EducationAchievementValidationChain {
    private static final Set<String> ACHIEVEMENT_ROLES = Set.of("R01", "R02", "R04", "R09");
    private static final Set<String> REJECTION_ACTIONS = Set.of("DEPARTMENT_REJECT", "CERTIFICATION_REJECT");

    /**
     * Validates the cross-cutting write guards in their required order. A date
     * outside the evaluation period is deliberately returned as a warning so a
     * valid achievement can still be stored.
     */
    public ValidationResult validate(ValidationContext context) {
        if (context.user() == null || context.user().roles() == null
                || context.user().roles().stream().noneMatch(ACHIEVEMENT_ROLES::contains)) {
            throw new ForbiddenException();
        }
        if (!context.dataScopeAllowed()) {
            throw new ForbiddenException();
        }
        if (!context.inputPeriodActive()) {
            throw new ConflictException("입력기간 안의 실적만 저장 또는 전이할 수 있습니다.");
        }
        if (context.evaluationConfirmed()) {
            throw new ConflictException("평가확정 데이터는 수정하거나 상태를 변경할 수 없습니다.");
        }

        List<String> warnings = new ArrayList<>();
        LocalDate occurredDate = context.occurredDate();
        if (occurredDate != null
                && ((context.evaluationPeriodStart() != null && occurredDate.isBefore(context.evaluationPeriodStart()))
                || (context.evaluationPeriodEnd() != null && occurredDate.isAfter(context.evaluationPeriodEnd())))) {
            warnings.add("업적발생일이 평가대상 기간 밖입니다. 경고를 확인한 뒤 저장할 수 있습니다.");
        }
        return new ValidationResult(List.copyOf(warnings));
    }

    /**
     * Validates required rejection metadata before any status lookup or write,
     * preventing incomplete rejection requests from changing achievement state.
     */
    public void validateTransitionInput(EducationAchievementTransitionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String actionType = request == null || request.actionType() == null
                ? ""
                : request.actionType().trim().toUpperCase();
        if (actionType.isBlank()) {
            fields.add(new ValidationError("actionType", "처리구분을 선택하세요."));
        }
        if (REJECTION_ACTIONS.contains(actionType)) {
            if (request.reasonCode() == null || request.reasonCode().isBlank()) {
                fields.add(new ValidationError("reasonCode", "반려 사유를 선택하세요."));
            }
            if (request.opinion() == null || request.opinion().isBlank()) {
                fields.add(new ValidationError("opinion", "반려 의견을 입력하세요."));
            }
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("교육영역 실적 상태 전이 요청이 올바르지 않습니다.", fields);
        }
    }

    /** Context supplied by a feature service after it resolves the target row and applicable periods. */
    public record ValidationContext(
            CurrentUser user,
            boolean dataScopeAllowed,
            boolean inputPeriodActive,
            boolean evaluationConfirmed,
            LocalDate occurredDate,
            LocalDate evaluationPeriodStart,
            LocalDate evaluationPeriodEnd) {
    }

    /** Non-blocking validation output that feature responses can expose as warnings. */
    public record ValidationResult(List<String> warnings) {
    }
}
