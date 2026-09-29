package com.example.faculty.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EducationAchievementPolicyTest {
    private final EducationAchievementValidationChain validationChain = new EducationAchievementValidationChain();
    private final EducationAchievementStateTransitionPolicy transitionPolicy = new EducationAchievementStateTransitionPolicy();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-29T06:00:00Z"), ZoneOffset.UTC);

    @Test
    void permitsOutOfPeriodOccurrenceWithWarningAfterAuthorizationScopeAndInputPeriodPass() {
        AchievementValidationResult result = validationChain.validate(new AchievementValidationRequest(
                true, Set.of("R01"), Set.of("KNUE-COL-EDU"), "KNUE-COL-EDU", true,
                AchievementCertificationStatus.DRAFTING, false));

        assertThat(result.warnings()).containsExactly("업적발생일이 평가대상 기간 밖입니다. 저장은 허용되며 평가 시 확인이 필요합니다.");
    }

    @Test
    void blocksMutationForMissingFunctionScopeInputPeriodOrEvaluationConfirmedStatus() {
        assertThatThrownBy(() -> validationChain.validate(new AchievementValidationRequest(
                false, Set.of("R01"), Set.of("KNUE-COL-EDU"), "KNUE-COL-EDU", true,
                AchievementCertificationStatus.DRAFTING, true)))
                .isInstanceOf(AchievementValidationException.class)
                .hasMessageContaining("기능 권한");
        assertThatThrownBy(() -> validationChain.validate(new AchievementValidationRequest(
                true, Set.of("R01"), Set.of("KNUE-COL-EDU"), "KNUE-COL-SCI", true,
                AchievementCertificationStatus.DRAFTING, true)))
                .isInstanceOf(AchievementValidationException.class)
                .hasMessageContaining("데이터 범위");
        assertThatThrownBy(() -> validationChain.validate(new AchievementValidationRequest(
                true, Set.of("R01"), Set.of("KNUE-COL-EDU"), "KNUE-COL-EDU", false,
                AchievementCertificationStatus.DRAFTING, true)))
                .isInstanceOf(AchievementValidationException.class)
                .hasMessageContaining("입력기간");
        assertThatThrownBy(() -> validationChain.validate(new AchievementValidationRequest(
                true, Set.of("R04"), Set.of("KNUE-COL-EDU"), "KNUE-COL-EDU", true,
                AchievementCertificationStatus.EVALUATION_CONFIRMED, true)))
                .isInstanceOf(AchievementValidationException.class)
                .hasMessageContaining("평가확정");
    }

    @Test
    void recordsAllowedTransitionAsImmutableStatusHistory() {
        EducationAchievementStatusHistory history = transitionPolicy.transition(new EducationAchievementTransitionRequest(
                AchievementCertificationStatus.SUBMITTED, AchievementCertificationStatus.DEPARTMENT_CONFIRMED,
                "R02", null, null), 22L, clock);

        assertThat(history.previousStatus()).isEqualTo(AchievementCertificationStatus.SUBMITTED);
        assertThat(history.nextStatus()).isEqualTo(AchievementCertificationStatus.DEPARTMENT_CONFIRMED);
        assertThat(history.actionType()).isEqualTo("CONFIRM");
        assertThat(history.processedBy()).isEqualTo(22L);
        assertThat(history.processedAt()).isEqualTo(Instant.parse("2026-09-29T06:00:00Z"));
    }

    @Test
    void requiresReasonOrOpinionForEveryRejectionTransition() {
        assertThatThrownBy(() -> transitionPolicy.transition(new EducationAchievementTransitionRequest(
                AchievementCertificationStatus.SUBMITTED, AchievementCertificationStatus.DEPARTMENT_REJECTED,
                "R02", " ", null), 22L, clock))
                .isInstanceOf(AchievementValidationException.class)
                .hasMessageContaining("사유 또는 의견");

        EducationAchievementStatusHistory history = transitionPolicy.transition(new EducationAchievementTransitionRequest(
                AchievementCertificationStatus.DEPARTMENT_CONFIRMED, AchievementCertificationStatus.CERTIFICATION_RETURNED,
                "R04", "CERTIFICATION_DEFECT", null), 44L, clock);
        assertThat(history.actionType()).isEqualTo("RETURN");
        assertThat(history.reasonCode()).isEqualTo("CERTIFICATION_DEFECT");
    }

    @Test
    void rejectsStateTransitionsOutsideTheDefinedPathAndWrongExecutorRole() {
        assertThatThrownBy(() -> transitionPolicy.transition(new EducationAchievementTransitionRequest(
                AchievementCertificationStatus.DRAFTING, AchievementCertificationStatus.CERTIFIED,
                "R01", null, null), 1L, clock))
                .isInstanceOf(AchievementValidationException.class)
                .hasMessageContaining("허용되지 않은");
        assertThatThrownBy(() -> transitionPolicy.transition(new EducationAchievementTransitionRequest(
                AchievementCertificationStatus.SUBMITTED, AchievementCertificationStatus.DEPARTMENT_CONFIRMED,
                "R01", null, null), 1L, clock))
                .isInstanceOf(AchievementValidationException.class)
                .hasMessageContaining("역할 권한");
    }
}
