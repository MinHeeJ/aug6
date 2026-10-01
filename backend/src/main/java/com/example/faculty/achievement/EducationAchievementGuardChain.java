package com.example.faculty.achievement;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;

import org.springframework.stereotype.Component;

/**
 * Applies the shared authorization, period, finalization-lock, occurrence-date, and state-transition
 * rules before an education-achievement service writes its source row or related history.
 */
@Component
public final class EducationAchievementGuardChain {
    private static final Set<String> ACHIEVEMENT_ROLES = Set.of("R01", "R02", "R04");
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "DRAFTING", Set.of("SUBMITTED"),
            "SUBMITTED", Set.of("DEPARTMENT_CONFIRMED", "DEPARTMENT_REJECTED"),
            "DEPARTMENT_REJECTED", Set.of("SUBMITTED"),
            "DEPARTMENT_CONFIRMED", Set.of("CERTIFIED", "CERTIFICATION_RETURNED"),
            "CERTIFICATION_RETURNED", Set.of("SUBMITTED"),
            "CERTIFIED", Set.of("EVALUATION_CONFIRMED"),
            "EVALUATION_CONFIRMED", Set.of("CERTIFIED"));

    private final Clock clock;

    /**
     * Creates a guard using the system clock for production state-history timestamps.
     */
    public EducationAchievementGuardChain() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Creates a guard with an explicit clock so state-history timestamps are deterministic in tests.
     *
     * @param clock clock used to stamp accepted transitions
     */
    public EducationAchievementGuardChain(Clock clock) {
        this.clock = clock;
    }

    /**
     * Validates the mandatory mutation gate in the required order and returns a warning when only the
     * achievement occurrence date falls outside the evaluation period.
     *
     * @param context request-derived authorization, period, status, and date facts
     * @return warning state that callers must include in their normal success response
     */
    public EducationAchievementGuardResult validateMutation(EducationAchievementMutationContext context) {
        requireContext(context);
        requireRole(context.roleCodes());
        requireDataScope(context.dataScopeAllowed());
        requireInputPeriod(context.inputPeriodActive());
        requireNotEvaluationConfirmed(context.certificationStatus());
        return occurrenceDateWarning(context.occurredDate(), context.evaluationPeriodStart(), context.evaluationPeriodEnd());
    }

    /**
     * Validates an education-achievement certification-state change and produces the append-only history
     * row data that the persistence layer must insert in the same transaction as the source-row update.
     *
     * @param request transition facts supplied by the service after its common mutation gate
     * @return immutable history data for the accepted transition
     */
    public EducationAchievementStatusHistory validateTransition(EducationAchievementTransitionRequest request) {
        if (request == null) {
            throw validation("상태 전이 요청을 입력하세요.", new ValidationError("request", "상태 전이 요청을 입력하세요."));
        }
        if (request.achievementId() == null || request.achievementId() <= 0) {
            throw validation("실적 식별자가 올바르지 않습니다.", new ValidationError("achievementId", "실적을 선택하세요."));
        }
        if (request.processedBy() == null || request.processedBy() <= 0) {
            throw validation("처리자 식별자가 올바르지 않습니다.", new ValidationError("processedBy", "처리자를 확인하세요."));
        }
        if (isBlank(request.achievementType()) || isBlank(request.previousStatus()) || isBlank(request.nextStatus())
                || isBlank(request.actionType())) {
            throw validation("상태 전이에 필요한 값을 입력하세요.", new ValidationError("transition", "상태 전이에 필요한 값을 입력하세요."));
        }
        String previousStatus = request.previousStatus().trim().toUpperCase();
        String nextStatus = request.nextStatus().trim().toUpperCase();
        if (!ALLOWED_TRANSITIONS.getOrDefault(previousStatus, Set.of()).contains(nextStatus)) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 허용되지 않은 상태 전이입니다.");
        }
        if (("DEPARTMENT_REJECTED".equals(nextStatus) || "CERTIFICATION_RETURNED".equals(nextStatus))
                && isBlank(request.reasonCode()) && isBlank(request.opinion())) {
            throw validation(
                    "반려 처리에는 사유 또는 의견이 필요합니다.",
                    new ValidationError("reasonCode", "반려 사유 또는 의견을 입력하세요.")
            );
        }
        return new EducationAchievementStatusHistory(
                request.achievementType().trim().toUpperCase(),
                request.achievementId(),
                previousStatus,
                nextStatus,
                request.actionType().trim().toUpperCase(),
                trimToNull(request.reasonCode()),
                trimToNull(request.opinion()),
                request.processedBy(),
                LocalDateTime.now(clock)
        );
    }

    private void requireContext(EducationAchievementMutationContext context) {
        if (context == null) {
            throw validation("업적 변경 요청을 입력하세요.", new ValidationError("request", "업적 변경 요청을 입력하세요."));
        }
    }

    private void requireRole(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.stream().noneMatch(ACHIEVEMENT_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireDataScope(boolean dataScopeAllowed) {
        if (!dataScopeAllowed) {
            throw new ForbiddenException();
        }
    }

    private void requireInputPeriod(boolean inputPeriodActive) {
        if (!inputPeriodActive) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 현재 입력기간에는 처리할 수 없습니다.");
        }
    }

    private void requireNotEvaluationConfirmed(String certificationStatus) {
        if ("EVALUATION_CONFIRMED".equals(normalize(certificationStatus))) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 변경할 수 없습니다.");
        }
    }

    private EducationAchievementGuardResult occurrenceDateWarning(
            LocalDate occurredDate,
            LocalDate evaluationPeriodStart,
            LocalDate evaluationPeriodEnd
    ) {
        if (occurredDate == null) {
            throw validation("업적발생일을 입력하세요.", new ValidationError("occurredDate", "업적발생일을 입력하세요."));
        }
        if (evaluationPeriodStart == null || evaluationPeriodEnd == null) {
            throw validation("평가대상 기간 설정이 필요합니다.", new ValidationError("evaluationPeriod", "평가대상 기간 설정을 확인하세요."));
        }
        if (evaluationPeriodEnd.isBefore(evaluationPeriodStart)) {
            throw validation("평가대상 기간 설정이 올바르지 않습니다.", new ValidationError("evaluationPeriod", "평가대상 기간의 시작과 종료를 확인하세요."));
        }
        boolean outsideEvaluationPeriod = occurredDate.isBefore(evaluationPeriodStart)
                || occurredDate.isAfter(evaluationPeriodEnd);
        return outsideEvaluationPeriod
                ? EducationAchievementGuardResult.warning("OCCURRED_DATE_OUTSIDE_EVALUATION_PERIOD")
                : EducationAchievementGuardResult.accepted();
    }

    private BusinessValidationException validation(String message, ValidationError error) {
        return new BusinessValidationException(message, List.of(error));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
