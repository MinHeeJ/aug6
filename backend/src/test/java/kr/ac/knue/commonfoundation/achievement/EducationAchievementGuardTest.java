package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

class EducationAchievementGuardTest {
    @Test
    void validatesPermittedTransitionAndReturnsAppendOnlyHistoryPayload() {
        EducationAchievementGuard guard = new EducationAchievementGuard(
                org.mockito.Mockito.mock(EducationAchievementGuardMapper.class)
        );

        EducationAchievementTransition transition = guard.validateTransition(
                "DEPARTMENT_CONFIRMED",
                "CERTIFY",
                null,
                "담당자 인증 완료"
        );

        assertThat(transition.previousStatus()).isEqualTo("DEPARTMENT_CONFIRMED");
        assertThat(transition.nextStatus()).isEqualTo("CERTIFIED");
        assertThat(transition.actionType()).isEqualTo("CERTIFY");
        assertThat(transition.opinion()).isEqualTo("담당자 인증 완료");
    }

    @Test
    void rejectsUnsupportedTransitionAndRequiresReasonAndOpinionForRejectionHistory() {
        EducationAchievementGuard guard = new EducationAchievementGuard(
                org.mockito.Mockito.mock(EducationAchievementGuardMapper.class)
        );

        assertThatThrownBy(() -> guard.validateTransition("DRAFT", "CERTIFY", null, null))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> guard.validateTransition(
                "SUBMITTED",
                "DEPARTMENT_REJECT",
                null,
                ""
        ))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("사유와 의견");
    }

    @Test
    void permitsOutOfEvaluationPeriodOccurrenceAsWarningAfterRequiredGuardsPass() {
        EducationAchievementGuardMapper mapper = org.mockito.Mockito.mock(
                EducationAchievementGuardMapper.class
        );
        EducationAchievementGuard guard = new EducationAchievementGuard(mapper);
        EducationAchievementCommandContext context = context(1001L, "DRAFT");
        when(mapper.existsOpenInputPeriod(eq("2026"), eq("KNUE-COL-EDU"), any(LocalDateTime.class)))
                .thenReturn(1);
        when(mapper.existsEvaluationFinalization(1001L, "2026")).thenReturn(0);
        when(mapper.existsOccurredDateWithinEvaluationPeriod(
                "2026",
                "KNUE-COL-EDU",
                LocalDate.of(2026, 2, 28)
        )).thenReturn(0);

        EducationAchievementGuardResult result = guard.validateMutation(r01(1001L), context);

        assertThat(result.occurredDateOutOfRangeWarning()).isTrue();
        verify(mapper).existsEvaluationFinalization(1001L, "2026");
    }

    @Test
    void deniesOtherTeachersDataAndPreventsFinalizedAchievementMutation() {
        EducationAchievementGuardMapper mapper = org.mockito.Mockito.mock(
                EducationAchievementGuardMapper.class
        );
        EducationAchievementGuard guard = new EducationAchievementGuard(mapper);

        assertThatThrownBy(() -> guard.validateMutation(r01(1001L), context(1002L, "DRAFT")))
                .isInstanceOf(ForbiddenException.class);

        EducationAchievementCommandContext finalized = context(1001L, "EVALUATION_CONFIRMED");
        when(mapper.existsOpenInputPeriod(eq("2026"), eq("KNUE-COL-EDU"), any(LocalDateTime.class)))
                .thenReturn(1);
        assertThatThrownBy(() -> guard.validateMutation(r01(1001L), finalized))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("EVALUATION_CONFIRMED");
    }

    private EducationAchievementCommandContext context(Long targetUserId, String currentStatus) {
        return new EducationAchievementCommandContext(
                targetUserId,
                "2026",
                "KNUE-COL-EDU",
                LocalDate.of(2026, 2, 28),
                currentStatus
        );
    }

    private CurrentUser r01(Long userId) {
        return new CurrentUser(userId, "professor1", "E1001", "교원", List.of("R01"), List.of());
    }
}
