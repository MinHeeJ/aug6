package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EducationAchievementFoundationTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-10T09:00:00Z"), ZoneOffset.UTC);

    @Test
    void allowedTransitionRecordsProcessorTimestampReasonAndStatusHistory() {
        EducationAchievementStatusHistoryPort historyPort = org.mockito.Mockito.mock(EducationAchievementStatusHistoryPort.class);
        EducationAchievementStatusTransitionService service = new EducationAchievementStatusTransitionService(historyPort, FIXED_CLOCK);

        EducationAchievementStatusTransition result = service.transition("LECTURE_EVALUATION", 81L,
                EducationAchievementStatus.DRAFTING, EducationAchievementStatus.SUBMITTED,
                "제출 완료", 1L, "REQ-B81-TRANSITION");

        ArgumentCaptor<EducationAchievementStatusTransition> captor = ArgumentCaptor.forClass(EducationAchievementStatusTransition.class);
        verify(historyPort).record(captor.capture());
        assertThat(result).isEqualTo(captor.getValue());
        assertThat(captor.getValue().previousStatus()).isEqualTo(EducationAchievementStatus.DRAFTING);
        assertThat(captor.getValue().nextStatus()).isEqualTo(EducationAchievementStatus.SUBMITTED);
        assertThat(captor.getValue().processedBy()).isEqualTo(1L);
        assertThat(captor.getValue().processedAt()).isEqualTo(LocalDateTime.parse("2026-04-10T09:00:00"));
        assertThat(captor.getValue().transitionReason()).isEqualTo("제출 완료");
    }

    @Test
    void invalidTransitionDoesNotRecordHistory() {
        EducationAchievementStatusHistoryPort historyPort = org.mockito.Mockito.mock(EducationAchievementStatusHistoryPort.class);
        EducationAchievementStatusTransitionService service = new EducationAchievementStatusTransitionService(historyPort, FIXED_CLOCK);

        assertThatThrownBy(() -> service.transition("LECTURE_EVALUATION", 81L,
                EducationAchievementStatus.DRAFTING, EducationAchievementStatus.CERTIFIED,
                "잘못된 전이", 1L, "REQ-B81-INVALID"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INVALID_STATE_TRANSITION");
        verify(historyPort, never()).record(any());
    }

    @Test
    void rejectionTransitionRequiresReasonBeforeHistorySideEffect() {
        EducationAchievementStatusHistoryPort historyPort = org.mockito.Mockito.mock(EducationAchievementStatusHistoryPort.class);
        EducationAchievementStatusTransitionService service = new EducationAchievementStatusTransitionService(historyPort, FIXED_CLOCK);

        assertThatThrownBy(() -> service.transition("LECTURE_EVALUATION", 81L,
                EducationAchievementStatus.SUBMITTED, EducationAchievementStatus.DEPARTMENT_REJECTED,
                " ", 2L, "REQ-B81-REJECT"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("반려 사유");
        verify(historyPort, never()).record(any());
    }

    @Test
    void validationChainAllowsWarningButBlocksOutOfScopePeriodAndConfirmedWritesInOrder() {
        EducationAchievementValidationPort validationPort = org.mockito.Mockito.mock(EducationAchievementValidationPort.class);
        EducationAchievementValidationService service = new EducationAchievementValidationService(validationPort, FIXED_CLOCK);
        CurrentUser teacher = new CurrentUser(1L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
        EducationAchievementValidationContext context = new EducationAchievementValidationContext(teacher, 1L,
                "2026", "KNUE-DEPT-COMP", "UNIT-EDU", LocalDate.parse("2026-01-15"));
        when(validationPort.hasDataScope(1L, 1L, "KNUE-DEPT-COMP", "UNIT-EDU")).thenReturn(true);
        when(validationPort.hasActiveInputPeriod("2026", "KNUE-DEPT-COMP", "UNIT-EDU", java.time.LocalDateTime.parse("2026-04-10T09:00:00"))).thenReturn(true);
        when(validationPort.hasEvaluationResultLock("2026", "KNUE-DEPT-COMP", "UNIT-EDU")).thenReturn(false);
        when(validationPort.isWithinEvaluationDate("2026", "KNUE-DEPT-COMP", LocalDate.parse("2026-01-15"))).thenReturn(false);

        assertThat(service.validateMutation(context).occurrenceDateWarning()).isTrue();

        org.mockito.Mockito.clearInvocations(validationPort);
        when(validationPort.hasDataScope(1L, 1L, "KNUE-DEPT-COMP", "UNIT-EDU")).thenReturn(false);
        assertThatThrownBy(() -> service.validateMutation(context)).isInstanceOf(ForbiddenException.class);
        verify(validationPort, never()).hasActiveInputPeriod("2026", "KNUE-DEPT-COMP", "UNIT-EDU", java.time.LocalDateTime.parse("2026-04-10T09:00:00"));

        org.mockito.Mockito.clearInvocations(validationPort);
        when(validationPort.hasDataScope(1L, 1L, "KNUE-DEPT-COMP", "UNIT-EDU")).thenReturn(true);
        when(validationPort.hasActiveInputPeriod("2026", "KNUE-DEPT-COMP", "UNIT-EDU", java.time.LocalDateTime.parse("2026-04-10T09:00:00"))).thenReturn(false);
        assertThatThrownBy(() -> service.validateMutation(context))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("PERIOD_NOT_ACTIVE");
        verify(validationPort, never()).hasEvaluationResultLock("2026", "KNUE-DEPT-COMP", "UNIT-EDU");

        org.mockito.Mockito.clearInvocations(validationPort);
        when(validationPort.hasActiveInputPeriod("2026", "KNUE-DEPT-COMP", "UNIT-EDU", java.time.LocalDateTime.parse("2026-04-10T09:00:00"))).thenReturn(true);
        when(validationPort.hasEvaluationResultLock("2026", "KNUE-DEPT-COMP", "UNIT-EDU")).thenReturn(true);
        assertThatThrownBy(() -> service.validateMutation(context))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");
        verify(validationPort, never()).isWithinEvaluationDate("2026", "KNUE-DEPT-COMP", LocalDate.parse("2026-01-15"));
    }
}
