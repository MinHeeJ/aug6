package com.example.faculty.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.Test;

class EducationAchievementStateTransitionTest {
    private final EducationAchievementGuardChain guard = new EducationAchievementGuardChain(
            Clock.fixed(Instant.parse("2026-04-15T09:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void permittedTransitionCreatesHistoryWithProcessorTimestampAndReason() {
        EducationAchievementStatusHistory history = guard.validateTransition(
                new EducationAchievementTransitionRequest(
                        "LECTURE_EVALUATION",
                        101L,
                        "SUBMITTED",
                        "DEPARTMENT_REJECTED",
                        "REJECT",
                        "INCOMPLETE_EVIDENCE",
                        "근거자료 보완이 필요합니다.",
                        2L
                )
        );

        assertThat(history.achievementType()).isEqualTo("LECTURE_EVALUATION");
        assertThat(history.previousStatus()).isEqualTo("SUBMITTED");
        assertThat(history.nextStatus()).isEqualTo("DEPARTMENT_REJECTED");
        assertThat(history.reasonCode()).isEqualTo("INCOMPLETE_EVIDENCE");
        assertThat(history.processedBy()).isEqualTo(2L);
        assertThat(history.processedAt()).isEqualTo(LocalDateTime.of(2026, 4, 15, 9, 0));
    }

    @Test
    void rejectionWithoutReasonOrOpinionIsRejectedBeforeHistoryCanBeProduced() {
        assertThatThrownBy(() -> guard.validateTransition(
                new EducationAchievementTransitionRequest(
                        "LECTURE_EVALUATION",
                        101L,
                        "DEPARTMENT_CONFIRMED",
                        "CERTIFICATION_RETURNED",
                        "RETURN",
                        null,
                        " ",
                        4L
                )
        ))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("사유 또는 의견");
    }

    @Test
    void disallowedTransitionIsRejectedAndEvaluationConfirmedRowsRemainLocked() {
        assertThatThrownBy(() -> guard.validateTransition(
                new EducationAchievementTransitionRequest(
                        "LECTURE_EVALUATION",
                        101L,
                        "DRAFTING",
                        "CERTIFIED",
                        "CERTIFY",
                        null,
                        "인증 요청",
                        4L
                )
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INVALID_STATE_TRANSITION");

        assertThatThrownBy(() -> guard.validateMutation(new EducationAchievementMutationContext(
                Set.of("R01"),
                true,
                true,
                "EVALUATION_CONFIRMED",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 31)
        )))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");
    }

    @Test
    void mutationGateRejectsUnauthorizedOutOfScopeAndOutOfPeriodRequests() {
        assertThatThrownBy(() -> guard.validateMutation(new EducationAchievementMutationContext(
                Set.of("R07"),
                true,
                true,
                "DRAFTING",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 31)
        )))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> guard.validateMutation(new EducationAchievementMutationContext(
                Set.of("R01"),
                false,
                true,
                "DRAFTING",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 31)
        )))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> guard.validateMutation(new EducationAchievementMutationContext(
                Set.of("R01"),
                true,
                false,
                "DRAFTING",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 31)
        )))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("PERIOD_NOT_ACTIVE");
    }

    @Test
    void occurredDateOutsideEvaluationPeriodWarnsButDoesNotBlockAuthorizedMutation() {
        EducationAchievementGuardResult result = guard.validateMutation(new EducationAchievementMutationContext(
                Set.of("R01"),
                true,
                true,
                "DRAFTING",
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 31)
        ));

        assertThat(result.warning()).isTrue();
        assertThat(result.warningCode()).isEqualTo("OCCURRED_DATE_OUTSIDE_EVALUATION_PERIOD");
    }
}
