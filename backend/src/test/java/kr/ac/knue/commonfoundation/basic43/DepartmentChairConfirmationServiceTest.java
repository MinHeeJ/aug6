package kr.ac.knue.commonfoundation.basic43;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

class DepartmentChairConfirmationServiceTest {
    @Test
    void transitionConfirmInsertsDepartmentConfirmationHistoryForReq1337Req1339() {
        DepartmentChairConfirmationMapper mapper = org.mockito.Mockito.mock(DepartmentChairConfirmationMapper.class);
        DepartmentChairConfirmationService service = new DepartmentChairConfirmationService(mapper);
        DepartmentChairConfirmationRow current = row("DEPARTMENT_REJECTED", "SUBMITTED");
        DepartmentChairConfirmationRow inserted = row("DEPARTMENT_CONFIRMED", "DEPARTMENT_CONFIRMED");
        when(mapper.findLatestByAchievementId(9001L)).thenReturn(current);
        when(mapper.activeDepartmentChairConfirmPeriodExists("2026", "DEPT-EDU", "EDUCATION")).thenReturn(1);
        when(mapper.transitionAllowed("SUBMITTED", "DEPARTMENT_CONFIRMED")).thenReturn(1);
        when(mapper.insertTransition(eq(9001L), eq("2026"), eq("DEPT-EDU"), eq("EDUCATION"),
                eq("DEPARTMENT_CONFIRMED"), eq("SUBMITTED"), eq("DEPARTMENT_CONFIRMED"),
                eq("확인"), eq(null), eq(1L), eq("학과장 확인 처리"))).thenReturn(inserted);

        DepartmentChairConfirmationRow result = service.transition(9001L,
                new BusinessTransitionRequest("CONFIRM", null, "확인", null), 1L);

        org.assertj.core.api.Assertions.assertThat(result.nextStatus()).isEqualTo("DEPARTMENT_CONFIRMED");
        verify(mapper).insertTransition(eq(9001L), eq("2026"), eq("DEPT-EDU"), eq("EDUCATION"),
                eq("DEPARTMENT_CONFIRMED"), eq("SUBMITTED"), eq("DEPARTMENT_CONFIRMED"),
                eq("확인"), eq(null), eq(1L), eq("학과장 확인 처리"));
    }

    @Test
    void transitionRejectRequiresReasonAndOpinionBeforeMapperSideEffectForReq1338Req1314() {
        DepartmentChairConfirmationMapper mapper = org.mockito.Mockito.mock(DepartmentChairConfirmationMapper.class);
        DepartmentChairConfirmationService service = new DepartmentChairConfirmationService(mapper);

        assertThatThrownBy(() -> service.transition(9001L, new BusinessTransitionRequest("REJECT", null, "", null), 1L))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insertTransition(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void transitionOutsideDepartmentChairConfirmPeriodKeepsTargetUnchangedForReq1339() {
        DepartmentChairConfirmationMapper mapper = org.mockito.Mockito.mock(DepartmentChairConfirmationMapper.class);
        DepartmentChairConfirmationService service = new DepartmentChairConfirmationService(mapper);
        when(mapper.findLatestByAchievementId(9001L)).thenReturn(row("DEPARTMENT_REJECTED", "SUBMITTED"));
        when(mapper.activeDepartmentChairConfirmPeriodExists("2026", "DEPT-EDU", "EDUCATION")).thenReturn(0);

        assertThatThrownBy(() -> service.transition(9001L, new BusinessTransitionRequest("CONFIRM", null, "확인", null), 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("확인기간");
        verify(mapper, never()).insertTransition(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private DepartmentChairConfirmationRow row(String confirmStatus, String nextStatus) {
        return new DepartmentChairConfirmationRow(301L, 9001L, "2026", "DEPT-EDU", "EDUCATION", confirmStatus,
                "SUBMITTED", nextStatus, "확인", null, 1L, LocalDateTime.parse("2026-09-02T09:00:00"), "학과장 확인 처리");
    }
}
