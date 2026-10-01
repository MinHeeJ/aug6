package kr.ac.knue.commonfoundation.basic81;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.Test;

class EducationAchievementFoundationTest {
    private final EducationAchievementStatusTransitionPolicy transitionPolicy =
            new EducationAchievementStatusTransitionPolicy();

    @Test
    void permittedTransitionReturnsCompleteStatusHistoryValue() {
        LocalDateTime processedAt = LocalDateTime.parse("2026-04-11T09:30:00");

        EducationAchievementStatusHistory history = transitionPolicy.transition(
                new EducationAchievementStatusTransitionRequest(
                        "LECTURE_EVALUATION",
                        81L,
                        EducationAchievementStatus.SUBMITTED,
                        EducationAchievementStatus.DEPARTMENT_CONFIRMED,
                        "DEPARTMENT_CONFIRM",
                        null,
                        "학과장 확인",
                        2L,
                        processedAt));

        assertThat(history.achievementType()).isEqualTo("LECTURE_EVALUATION");
        assertThat(history.achievementId()).isEqualTo(81L);
        assertThat(history.previousStatus()).isEqualTo(EducationAchievementStatus.SUBMITTED);
        assertThat(history.nextStatus()).isEqualTo(EducationAchievementStatus.DEPARTMENT_CONFIRMED);
        assertThat(history.actionType()).isEqualTo("DEPARTMENT_CONFIRM");
        assertThat(history.processedBy()).isEqualTo(2L);
        assertThat(history.processedAt()).isEqualTo(processedAt);
    }

    @Test
    void rejectionTransitionRequiresReasonOrOpinion() {
        assertThatThrownBy(() -> transitionPolicy.transition(
                new EducationAchievementStatusTransitionRequest(
                        "LECTURE_EVALUATION",
                        81L,
                        EducationAchievementStatus.SUBMITTED,
                        EducationAchievementStatus.DEPARTMENT_REJECTED,
                        "DEPARTMENT_REJECT",
                        " ",
                        null,
                        2L,
                        LocalDateTime.parse("2026-04-11T09:30:00"))))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("반려 처리");
    }

    @Test
    void transitionPolicyRejectsRoutesOutsideTheDefinedLifecycle() {
        assertThatThrownBy(() -> transitionPolicy.transition(
                new EducationAchievementStatusTransitionRequest(
                        "LECTURE_EVALUATION",
                        81L,
                        EducationAchievementStatus.DRAFT,
                        EducationAchievementStatus.CERTIFIED,
                        "CERTIFY",
                        null,
                        "임의 인증",
                        4L,
                        LocalDateTime.parse("2026-04-11T09:30:00"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("허용되지 않은");
    }

    @Test
    void guardReturnsWarningForOutsideEvaluationDateAfterAuthorizationAndMutationLocksPass() {
        EducationAchievementGuardMapper mapper = org.mockito.Mockito.mock(
                EducationAchievementGuardMapper.class);
        EducationAchievementGuardService service = new EducationAchievementGuardService(mapper);
        CurrentUser r01 = new CurrentUser(
                101L,
                "faculty",
                "E0101",
                "교원",
                List.of("R01"),
                List.of());
        EducationAchievementMutationContext context = new EducationAchievementMutationContext(
                101L,
                "2026",
                LocalDate.parse("2025-12-31"));
        when(mapper.countActiveInputPeriods("2026", 101L)).thenReturn(1);
        when(mapper.countEvaluationConfirmations(101L, "2026")).thenReturn(0);
        when(mapper.countEvaluationDatePeriods("2026", 101L, LocalDate.parse("2025-12-31")))
                .thenReturn(0);

        OccurredDateValidation result = service.validateMutation(r01, context);

        assertThat(result.warning()).isTrue();
        assertThat(result.message()).contains("평가대상 기간 밖");
    }

    @Test
    void guardBlocksConfirmedRowBeforeOccurredDateWarningIsCalculated() {
        EducationAchievementGuardMapper mapper = org.mockito.Mockito.mock(
                EducationAchievementGuardMapper.class);
        EducationAchievementGuardService service = new EducationAchievementGuardService(mapper);
        CurrentUser r01 = new CurrentUser(
                101L,
                "faculty",
                "E0101",
                "교원",
                List.of("R01"),
                List.of());
        EducationAchievementMutationContext context = new EducationAchievementMutationContext(
                101L,
                "2026",
                LocalDate.parse("2026-04-10"));
        when(mapper.countActiveInputPeriods("2026", 101L)).thenReturn(1);
        when(mapper.countEvaluationConfirmations(101L, "2026")).thenReturn(1);

        assertThatThrownBy(() -> service.validateMutation(r01, context))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).countEvaluationDatePeriods(any(), any(), any());
    }

    @Test
    void guardRejectsR01AttemptToMutateAnotherFacultyMembersAchievementBeforePeriodLookup() {
        EducationAchievementGuardMapper mapper = org.mockito.Mockito.mock(
                EducationAchievementGuardMapper.class);
        EducationAchievementGuardService service = new EducationAchievementGuardService(mapper);
        CurrentUser r01 = new CurrentUser(
                101L,
                "faculty",
                "E0101",
                "교원",
                List.of("R01"),
                List.of());
        EducationAchievementMutationContext context = new EducationAchievementMutationContext(
                102L,
                "2026",
                LocalDate.parse("2026-04-10"));

        assertThatThrownBy(() -> service.validateMutation(r01, context))
                .isInstanceOf(ForbiddenException.class);

        verify(mapper, never()).countActiveInputPeriods(any(), any());
        verify(mapper, never()).countEvaluationConfirmations(any(), any());
    }

    @Test
    void guardAllowsR02AndR04OnlyWhenTheirDatabaseDataScopeMatchesTheTarget() {
        EducationAchievementGuardMapper mapper = org.mockito.Mockito.mock(
                EducationAchievementGuardMapper.class);
        EducationAchievementGuardService service = new EducationAchievementGuardService(mapper);
        EducationAchievementMutationContext context = new EducationAchievementMutationContext(
                202L,
                "2026",
                LocalDate.parse("2026-04-10"));
        CurrentUser r02 = new CurrentUser(
                102L,
                "department-head",
                "E0102",
                "학과장",
                List.of("R02"),
                List.of());
        CurrentUser r04 = new CurrentUser(
                104L,
                "certifier",
                "E0104",
                "인증담당자",
                List.of("R04"),
                List.of());
        when(mapper.countSharedActiveOrganization(102L, 202L)).thenReturn(1);
        when(mapper.countCertificationScope(104L, 202L)).thenReturn(1);
        when(mapper.countActiveInputPeriods("2026", 202L)).thenReturn(1);
        when(mapper.countEvaluationConfirmations(202L, "2026")).thenReturn(0);
        when(mapper.countEvaluationDatePeriods("2026", 202L, LocalDate.parse("2026-04-10")))
                .thenReturn(1);

        assertThat(service.validateMutation(r02, context).warning()).isFalse();
        assertThat(service.validateMutation(r04, context).warning()).isFalse();
    }
}
