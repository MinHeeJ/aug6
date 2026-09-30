package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EducationAchievementStatusTransitionTest {
    private final EducationAchievementAccessMapper mapper = Mockito.mock(EducationAchievementAccessMapper.class);
    private final EducationAchievementAccessValidator validator = new EducationAchievementAccessValidator(
            mapper,
            Clock.fixed(Instant.parse("2026-04-15T09:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void permittedTransitionBuildsCompleteStatusHistoryPayload() {
        LocalDateTime processedAt = LocalDateTime.parse("2026-04-15T09:00:00");

        EducationAchievementStatusTransition transition = validator.prepareStatusTransition(
                "LECTURE_EVALUATION",
                790001L,
                "DRAFTING",
                "SUBMITTED",
                "SUBMIT",
                null,
                "제출합니다.",
                2L,
                processedAt
        );

        assertThat(transition.achievementType()).isEqualTo("LECTURE_EVALUATION");
        assertThat(transition.previousStatus()).isEqualTo("DRAFTING");
        assertThat(transition.nextStatus()).isEqualTo("SUBMITTED");
        assertThat(transition.actionType()).isEqualTo("SUBMIT");
        assertThat(transition.processedBy()).isEqualTo(2L);
        assertThat(transition.processedAt()).isEqualTo(processedAt);
    }

    @Test
    void invalidTransitionIsRejectedWithoutCreatingHistoryPayload() {
        assertThatThrownBy(() -> validator.prepareStatusTransition(
                "LECTURE_EVALUATION",
                790001L,
                "DRAFTING",
                "CERTIFIED",
                "CERTIFY",
                null,
                null,
                2L,
                LocalDateTime.parse("2026-04-15T09:00:00")
        )).isInstanceOf(BusinessValidationException.class)
                .satisfies(exception -> assertThat(((BusinessValidationException) exception).fields())
                        .extracting(field -> field.field())
                        .contains("nextStatus"));
    }

    @Test
    void rejectionRequiresReasonOrOpinionBeforeHistoryCanBePersisted() {
        assertThatThrownBy(() -> validator.prepareStatusTransition(
                "LECTURE_EVALUATION",
                790002L,
                "SUBMITTED",
                "DEPARTMENT_REJECTED",
                "DEPARTMENT_REJECT",
                null,
                " ",
                2L,
                LocalDateTime.parse("2026-04-15T09:00:00")
        )).isInstanceOf(BusinessValidationException.class)
                .satisfies(exception -> assertThat(((BusinessValidationException) exception).fields())
                        .extracting(field -> field.field())
                        .contains("reasonCode"));
    }

    @Test
    void r01CannotWriteAnotherTeachersAchievement() {
        CurrentUser teacher = new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
        AchievementWriteContext context = new AchievementWriteContext(
                3L,
                "2026",
                "KNUE-DEPT-COMP",
                LocalDate.parse("2026-03-15"),
                "DRAFTING"
        );

        assertThatThrownBy(() -> validator.validateWrite(teacher, context))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ForbiddenException.class);
        Mockito.verifyNoInteractions(mapper);
    }

    @Test
    void writeChainRejectsInactivePeriodBeforeFinalizationOrDateChecks() {
        CurrentUser teacher = new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
        AchievementWriteContext context = new AchievementWriteContext(
                2L,
                "2026",
                "KNUE-DEPT-COMP",
                LocalDate.parse("2026-03-15"),
                "DRAFTING"
        );

        assertThatThrownBy(() -> validator.validateWrite(teacher, context))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ConflictException.class)
                .hasMessageContaining("PERIOD_NOT_ACTIVE");
        Mockito.verify(mapper).countActiveInputPeriod(
                "2026",
                "KNUE-DEPT-COMP",
                LocalDateTime.parse("2026-04-15T09:00:00")
        );
        Mockito.verifyNoMoreInteractions(mapper);
    }

    @Test
    void writeChainBlocksFinalizedAchievementAfterActivePeriodCheck() {
        CurrentUser teacher = new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
        AchievementWriteContext context = new AchievementWriteContext(
                2L,
                "2026",
                "KNUE-DEPT-COMP",
                LocalDate.parse("2026-03-15"),
                "DRAFTING"
        );
        Mockito.when(mapper.countActiveInputPeriod(
                "2026",
                "KNUE-DEPT-COMP",
                LocalDateTime.parse("2026-04-15T09:00:00")
        )).thenReturn(1);
        Mockito.when(mapper.countEvaluationFinalizationLock(2L, "2026")).thenReturn(1);

        assertThatThrownBy(() -> validator.validateWrite(teacher, context))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");
        Mockito.verify(mapper, Mockito.never()).countOccurredDateInEvaluationPeriod(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        );
    }

    @Test
    void writeChainAllowsOnlyDateWarningAfterRoleScopePeriodAndLockChecksPass() {
        CurrentUser teacher = new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
        AchievementWriteContext context = new AchievementWriteContext(
                2L,
                "2026",
                "KNUE-DEPT-COMP",
                LocalDate.parse("2026-02-28"),
                "DRAFTING"
        );
        Mockito.when(mapper.countActiveInputPeriod(
                "2026",
                "KNUE-DEPT-COMP",
                LocalDateTime.parse("2026-04-15T09:00:00")
        )).thenReturn(1);
        Mockito.when(mapper.countEvaluationFinalizationLock(2L, "2026")).thenReturn(0);
        Mockito.when(mapper.countOccurredDateInEvaluationPeriod(
                "2026",
                "KNUE-DEPT-COMP",
                LocalDate.parse("2026-02-28")
        )).thenReturn(0);

        EducationAchievementAccessDecision decision = validator.validateWrite(teacher, context);

        assertThat(decision.hasOccurredDateWarning()).isTrue();
        assertThat(decision.warnings()).containsExactly("OCCURRED_DATE_OUTSIDE_EVALUATION_PERIOD");
    }
}
