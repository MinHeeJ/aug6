package com.example.faculty.achievement;

import java.time.Clock;
import java.util.Map;

/**
 * 교육영역 실적의 허용 상태 경로와 역할별 처리 책임, 반려 근거 필수 규칙을 보장한다.
 */
public final class EducationAchievementStateTransitionPolicy {
    private static final Map<Transition, TransitionRule> TRANSITIONS = Map.of(
            new Transition(AchievementCertificationStatus.DRAFTING, AchievementCertificationStatus.SUBMITTED), new TransitionRule("R01", "SUBMIT", false),
            new Transition(AchievementCertificationStatus.SUBMITTED, AchievementCertificationStatus.DEPARTMENT_CONFIRMED), new TransitionRule("R02", "CONFIRM", false),
            new Transition(AchievementCertificationStatus.SUBMITTED, AchievementCertificationStatus.DEPARTMENT_REJECTED), new TransitionRule("R02", "REJECT", true),
            new Transition(AchievementCertificationStatus.DEPARTMENT_REJECTED, AchievementCertificationStatus.SUBMITTED), new TransitionRule("R01", "SUBMIT", false),
            new Transition(AchievementCertificationStatus.DEPARTMENT_CONFIRMED, AchievementCertificationStatus.CERTIFIED), new TransitionRule("R04", "CERTIFY", false),
            new Transition(AchievementCertificationStatus.DEPARTMENT_CONFIRMED, AchievementCertificationStatus.CERTIFICATION_RETURNED), new TransitionRule("R04", "RETURN", true),
            new Transition(AchievementCertificationStatus.CERTIFICATION_RETURNED, AchievementCertificationStatus.SUBMITTED), new TransitionRule("R01", "SUBMIT", false),
            new Transition(AchievementCertificationStatus.CERTIFIED, AchievementCertificationStatus.EVALUATION_CONFIRMED), new TransitionRule("R04", "EVALUATE", false),
            new Transition(AchievementCertificationStatus.EVALUATION_CONFIRMED, AchievementCertificationStatus.CERTIFIED), new TransitionRule("R04", "CANCEL_EVALUATION", false));

    /**
     * 허용 상태 전이만 처리하고, 반려 상태에는 사유코드 또는 의견을 요구한 뒤 상태 이력 값을 만든다.
     */
    public EducationAchievementStatusHistory transition(EducationAchievementTransitionRequest request, long processedBy, Clock clock) {
        if (request == null || request.previousStatus() == null || request.nextStatus() == null || clock == null) {
            throw new AchievementValidationException("상태 전이 정보가 완전하지 않습니다.");
        }
        TransitionRule rule = TRANSITIONS.get(new Transition(request.previousStatus(), request.nextStatus()));
        if (rule == null) {
            throw new AchievementValidationException("허용되지 않은 인증 상태 전이입니다.");
        }
        if (!rule.requiredRole().equals(request.requesterRole())) {
            throw new AchievementValidationException("해당 상태 전이를 처리할 역할 권한이 없습니다.");
        }
        if (rule.rejectionReasonRequired() && !hasText(request.reasonCode()) && !hasText(request.opinion())) {
            throw new AchievementValidationException("반려 처리에는 사유 또는 의견을 입력해야 합니다.");
        }
        return new EducationAchievementStatusHistory(request.previousStatus(), request.nextStatus(), rule.actionType(),
                trimToNull(request.reasonCode()), trimToNull(request.opinion()), processedBy, clock.instant());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private record Transition(AchievementCertificationStatus previous, AchievementCertificationStatus next) {
    }

    private record TransitionRule(String requiredRole, String actionType, boolean rejectionReasonRequired) {
    }
}
