package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;

/**
 * Applies the shared education-achievement write guards in the required order before a
 * service persists a source record, detail records, and their history in one transaction.
 */
public class EducationAchievementGuardChain {
    private static final List<String> WRITE_ROLES = List.of("R01", "R02", "R04", "R09");

    /**
     * Enforces role, data-scope, input-period, finalization-lock, and occurred-date rules.
     * The caller supplies the data-scope and period facts from the established persistence
     * adapters so this class remains a deterministic business-rule boundary.
     */
    public EducationAchievementGuardResult validateWrite(
            CurrentUser actor,
            Long targetUserId,
            boolean dataScopeAllowed,
            boolean inputPeriodActive,
            boolean evaluationConfirmed,
            LocalDate occurredDate,
            LocalDate evaluationPeriodStart,
            LocalDate evaluationPeriodEnd
    ) {
        requireAuthorizedActor(actor, targetUserId, dataScopeAllowed);
        if (!inputPeriodActive) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 현재 입력기간에는 교육영역 실적을 저장할 수 없습니다.");
        }
        if (evaluationConfirmed) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 대상 실적은 수정하거나 삭제할 수 없습니다.");
        }
        validateOccurredDate(occurredDate, evaluationPeriodStart, evaluationPeriodEnd);
        return new EducationAchievementGuardResult(
                occurredDateWarning(occurredDate, evaluationPeriodStart, evaluationPeriodEnd),
                Instant.now()
        );
    }

    /**
     * Validates the invariant education-achievement status graph and returns an immutable
     * append-only history entry for the later transactional persistence step.
     */
    public EducationAchievementStatusHistory transition(
            String achievementType,
            Long achievementId,
            String currentStatus,
            String nextStatus,
            String reasonCode,
            String opinion,
            Long processedBy
    ) {
        List<ValidationError> fields = new ArrayList<>();
        if (!EducationAchievementType.isSupported(achievementType)) {
            fields.add(new ValidationError("achievementType", "교육영역 실적 유형이 올바르지 않습니다."));
        }
        if (achievementId == null || achievementId <= 0) {
            fields.add(new ValidationError("achievementId", "실적 식별자가 올바르지 않습니다."));
        }
        if (processedBy == null || processedBy <= 0) {
            fields.add(new ValidationError("processedBy", "처리자 식별자가 올바르지 않습니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("교육영역 실적 상태 전이 요청이 올바르지 않습니다.", fields);
        }

        EducationAchievementStatus from = EducationAchievementStatus.from(currentStatus, "currentStatus");
        EducationAchievementStatus to = EducationAchievementStatus.from(nextStatus, "nextStatus");
        if (!from.canTransitionTo(to)) {
            throw new ConflictException(
                    "INVALID_STATE_TRANSITION: " + from.name() + "에서 " + to.name() + " 상태로 전이할 수 없습니다."
            );
        }
        if (to.requiresOpinion() && (opinion == null || opinion.isBlank())) {
            throw new BusinessValidationException(
                    "반려 상태 전이에는 사유 또는 의견이 필요합니다.",
                    List.of(new ValidationError("opinion", "학과장미승인 또는 인증반려에는 의견을 입력하세요."))
            );
        }
        return new EducationAchievementStatusHistory(
                EducationAchievementType.normalize(achievementType),
                achievementId,
                from.name(),
                to.name(),
                actionTypeFor(from, to),
                blankToNull(reasonCode),
                blankToNull(opinion),
                processedBy,
                Instant.now()
        );
    }

    private String actionTypeFor(
            EducationAchievementStatus from,
            EducationAchievementStatus to
    ) {
        if (from == EducationAchievementStatus.EVALUATION_CONFIRMED
                && to == EducationAchievementStatus.CERTIFIED) {
            return "EVALUATION_CANCEL";
        }
        return to.actionType();
    }

    private void requireAuthorizedActor(CurrentUser actor, Long targetUserId, boolean dataScopeAllowed) {
        if (actor == null || actor.userId() == null || actor.roles() == null) {
            throw new ForbiddenException();
        }
        boolean hasWriteRole = actor.roles().stream().filter(Objects::nonNull).anyMatch(WRITE_ROLES::contains);
        if (!hasWriteRole) {
            throw new ForbiddenException();
        }
        if (actor.roles().contains("R01") && !Objects.equals(actor.userId(), targetUserId)) {
            throw new ForbiddenException();
        }
        if (!dataScopeAllowed) {
            throw new ForbiddenException();
        }
    }

    private void validateOccurredDate(
            LocalDate occurredDate,
            LocalDate evaluationPeriodStart,
            LocalDate evaluationPeriodEnd
    ) {
        if (occurredDate == null) {
            throw new BusinessValidationException(
                    "교육영역 실적 입력값이 올바르지 않습니다.",
                    List.of(new ValidationError("occurredDate", "업적발생일을 입력하세요."))
            );
        }
        if ((evaluationPeriodStart == null) != (evaluationPeriodEnd == null)) {
            throw new IllegalArgumentException("평가대상 기간은 시작일과 종료일을 함께 제공해야 합니다.");
        }
        if (evaluationPeriodStart != null && evaluationPeriodStart.isAfter(evaluationPeriodEnd)) {
            throw new IllegalArgumentException("평가대상 기간의 시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    private boolean occurredDateWarning(
            LocalDate occurredDate,
            LocalDate evaluationPeriodStart,
            LocalDate evaluationPeriodEnd
    ) {
        return evaluationPeriodStart != null
                && (occurredDate.isBefore(evaluationPeriodStart) || occurredDate.isAfter(evaluationPeriodEnd));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Result consumed by the write service to expose a non-blocking occurred-date warning. */
    public record EducationAchievementGuardResult(boolean occurredDateWarning, Instant evaluatedAt) {
    }

    /** Immutable data required to persist one education-achievement status history row. */
    public record EducationAchievementStatusHistory(
            String achievementType,
            Long achievementId,
            String previousStatus,
            String nextStatus,
            String actionType,
            String reasonCode,
            String opinion,
            Long processedBy,
            Instant processedAt
    ) {
    }

    private enum EducationAchievementType {
        LECTURE_EVALUATION,
        LECTURE,
        STUDENT_GUIDANCE,
        DEGREE_COMPLETION;

        static boolean isSupported(String value) {
            try {
                valueOf(normalize(value));
                return true;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }

        static String normalize(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("교육영역 실적 유형이 필요합니다.");
            }
            return value.trim().toUpperCase();
        }
    }

    private enum EducationAchievementStatus {
        DRAFTING,
        SUBMITTED,
        DEPARTMENT_CONFIRMED,
        DEPARTMENT_REJECTED,
        CERTIFIED,
        CERTIFICATION_RETURNED,
        EVALUATION_CONFIRMED,
        DELETED;

        static EducationAchievementStatus from(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new BusinessValidationException(
                        "교육영역 실적 상태 전이 요청이 올바르지 않습니다.",
                        List.of(new ValidationError(field, "상태를 입력하세요."))
                );
            }
            try {
                return valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new BusinessValidationException(
                        "교육영역 실적 상태 전이 요청이 올바르지 않습니다.",
                        List.of(new ValidationError(field, "지원하지 않는 상태입니다."))
                );
            }
        }

        boolean canTransitionTo(EducationAchievementStatus next) {
            return switch (this) {
                case DRAFTING -> next == SUBMITTED;
                case SUBMITTED -> next == DEPARTMENT_CONFIRMED || next == DEPARTMENT_REJECTED;
                case DEPARTMENT_REJECTED -> next == SUBMITTED;
                case DEPARTMENT_CONFIRMED -> next == CERTIFIED || next == CERTIFICATION_RETURNED;
                case CERTIFICATION_RETURNED -> next == SUBMITTED;
                case CERTIFIED -> next == EVALUATION_CONFIRMED;
                case EVALUATION_CONFIRMED -> next == CERTIFIED;
                case DELETED -> false;
            };
        }

        boolean requiresOpinion() {
            return this == DEPARTMENT_REJECTED || this == CERTIFICATION_RETURNED;
        }

        String actionType() {
            return switch (this) {
                case SUBMITTED -> "SUBMIT";
                case DEPARTMENT_CONFIRMED -> "DEPARTMENT_CONFIRM";
                case DEPARTMENT_REJECTED -> "DEPARTMENT_REJECT";
                case CERTIFIED -> "CERTIFY";
                case CERTIFICATION_RETURNED -> "CERTIFICATION_RETURN";
                case EVALUATION_CONFIRMED -> "EVALUATION_CONFIRM";
                case DRAFTING, DELETED -> throw new IllegalStateException("상태 전이 이력 actionType이 정의되지 않았습니다.");
            };
        }
    }
}
