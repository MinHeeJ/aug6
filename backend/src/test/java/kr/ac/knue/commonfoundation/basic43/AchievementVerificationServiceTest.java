package kr.ac.knue.commonfoundation.basic43;

import static org.assertj.core.api.Assertions.assertThat;
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

class AchievementVerificationServiceTest {
    @Test
    void transitionCertifyInsertsVerificationHistoryWithEvidenceForReq1342Req1343() {
        AchievementVerificationMapper mapper = org.mockito.Mockito.mock(AchievementVerificationMapper.class);
        AchievementVerificationService service = new AchievementVerificationService(mapper);
        AchievementVerificationRow current = row("CERTIFY", "DEPARTMENT_CONFIRMED", "DEPARTMENT_CONFIRMED");
        AchievementVerificationRow inserted = row("CERTIFY", "DEPARTMENT_CONFIRMED", "CERTIFIED");
        when(mapper.findLatestByAchievementId(9101L)).thenReturn(current);
        when(mapper.handlerScopeExists(9101L, 1L)).thenReturn(1);
        when(mapper.transitionAllowed("DEPARTMENT_CONFIRMED", "CERTIFIED")).thenReturn(1);
        when(mapper.insertTransition(eq(9101L), eq("2026"), eq(1L), eq("CERTIFY"),
                eq("DEPARTMENT_CONFIRMED"), eq("CERTIFIED"), eq("인증"), eq("FILE-9101"),
                eq(null), eq(1L), eq("담당자 인증 처리"))).thenReturn(inserted);

        AchievementVerificationRow result = service.transition(9101L,
                new BusinessTransitionRequest("CERTIFY", null, "인증", "FILE-9101"), 1L);

        assertThat(result.nextStatus()).isEqualTo("CERTIFIED");
        verify(mapper).insertTransition(eq(9101L), eq("2026"), eq(1L), eq("CERTIFY"),
                eq("DEPARTMENT_CONFIRMED"), eq("CERTIFIED"), eq("인증"), eq("FILE-9101"),
                eq(null), eq(1L), eq("담당자 인증 처리"));
    }

    @Test
    void transitionReturnRequiresReasonOpinionAndEvidenceBeforeMapperSideEffectForReq1343Req1314() {
        AchievementVerificationMapper mapper = org.mockito.Mockito.mock(AchievementVerificationMapper.class);
        AchievementVerificationService service = new AchievementVerificationService(mapper);

        assertThatThrownBy(() -> service.transition(9101L, new BusinessTransitionRequest("RETURN", null, "", null), 1L))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insertTransition(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void transitionCancelCertificationReturnsToDepartmentConfirmedAndRejectsSubmittedFallbackForReq1346() {
        AchievementVerificationMapper mapper = org.mockito.Mockito.mock(AchievementVerificationMapper.class);
        AchievementVerificationService service = new AchievementVerificationService(mapper);
        when(mapper.findLatestByAchievementId(9101L)).thenReturn(row("CERTIFY", "DEPARTMENT_CONFIRMED", "CERTIFIED"));
        when(mapper.handlerScopeExists(9101L, 1L)).thenReturn(1);
        when(mapper.insertTransition(eq(9101L), eq("2026"), eq(1L), eq("CANCEL_CERTIFICATION"),
                eq("CERTIFIED"), eq("DEPARTMENT_CONFIRMED"), eq("재검토"), eq("FILE-9101"),
                eq(null), eq(1L), eq("담당자 인증 처리")))
                .thenReturn(row("CANCEL_CERTIFICATION", "CERTIFIED", "DEPARTMENT_CONFIRMED"));

        AchievementVerificationRow result = service.transition(9101L,
                new BusinessTransitionRequest("CANCEL_CERTIFICATION", null, "재검토", "FILE-9101"), 1L);

        assertThat(result.nextStatus()).isEqualTo("DEPARTMENT_CONFIRMED");

        when(mapper.findLatestByAchievementId(9102L)).thenReturn(row("CERTIFY", "SUBMITTED", "CERTIFIED"));
        when(mapper.handlerScopeExists(9102L, 1L)).thenReturn(1);
        assertThatThrownBy(() -> service.transition(9102L,
                new BusinessTransitionRequest("CANCEL_CERTIFICATION", null, "재검토", "FILE-9102"), 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("직전 검토 상태");
    }

    private AchievementVerificationRow row(String actionType, String previousStatus, String nextStatus) {
        return new AchievementVerificationRow(501L, 9101L, "2026", 1L, actionType,
                previousStatus, nextStatus, "인증", "FILE-9101", null, 1L,
                LocalDateTime.parse("2026-09-02T09:00:00"), "담당자 인증 처리");
    }
}
