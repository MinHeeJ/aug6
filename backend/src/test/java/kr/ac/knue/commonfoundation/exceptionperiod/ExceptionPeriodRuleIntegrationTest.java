package kr.ac.knue.commonfoundation.exceptionperiod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ExceptionPeriodRuleIntegrationTest {
    @Test
    void exceptionPeriodTakesPrecedenceOverBasic35ModificationPeriodOutsideGeneralWindow() {
        ExceptionPeriodMapper mapper = org.mockito.Mockito.mock(ExceptionPeriodMapper.class);
        ExceptionPeriodService service = new ExceptionPeriodService(mapper);
        LocalDateTime requestAt = LocalDateTime.parse("2026-08-02T09:00:00");
        ExceptionPeriodRow exceptionRow = new ExceptionPeriodRow(701L, "2026", 2L, "홍길동", "EDUCATION", "MODIFY_ACHIEVEMENT",
                LocalDateTime.parse("2026-08-01T09:00:00"), LocalDateTime.parse("2026-08-05T18:00:00"),
                "출장 승인", "Y", 1L, 9L, requestAt, requestAt, "예외 승인");
        when(mapper.findActiveExceptionPeriodForModification(2L, "EDUCATION", "MODIFY_ACHIEVEMENT", requestAt)).thenReturn(exceptionRow);

        ExceptionPeriodDecision decision = service.evaluateModificationAccess("2026", 2L, "EDUCATION", "MODIFY_ACHIEVEMENT", "KNUE-COL-EDU", requestAt, "인증");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.exceptionApplied()).isTrue();
        assertThat(decision.settingId()).isEqualTo(701L);
        verify(mapper, never()).countActiveModificationPeriods(any(), any(), any(), any());
    }

    @Test
    void finalConfirmedStatusBlocksModificationBeforeExceptionLookup() {
        ExceptionPeriodMapper mapper = org.mockito.Mockito.mock(ExceptionPeriodMapper.class);
        ExceptionPeriodService service = new ExceptionPeriodService(mapper);

        ExceptionPeriodDecision decision = service.evaluateModificationAccess("2026", 2L, "EDUCATION", "MODIFY_ACHIEVEMENT", "KNUE-COL-EDU", LocalDateTime.parse("2026-08-02T09:00:00"), "평가확정");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("평가확정");
        verify(mapper, never()).findActiveExceptionPeriodForModification(any(), any(), any(), any());
        verify(mapper, never()).countActiveModificationPeriods(any(), any(), any(), any());
    }
}
