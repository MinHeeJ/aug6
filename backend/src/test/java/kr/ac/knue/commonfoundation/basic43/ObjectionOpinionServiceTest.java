package kr.ac.knue.commonfoundation.basic43;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

class ObjectionOpinionServiceTest {
    @Test
    void transitionAcceptInsertsDecisionHistoryWithoutOriginalScoreSideEffectForReq1354Req1358() {
        ObjectionOpinionMapper mapper = org.mockito.Mockito.mock(ObjectionOpinionMapper.class);
        ObjectionOpinionService service = new ObjectionOpinionService(mapper);
        ObjectionOpinionRow current = row("NEEDS_REVIEW");
        ObjectionOpinionRow inserted = row("ACCEPTED");
        when(mapper.findLatestByObjectionId(9301L)).thenReturn(current);
        when(mapper.objectionScopeExists(9301L, 1L)).thenReturn(1);
        when(mapper.insertTransition(eq(9301L), eq("2026"), eq(2L), eq("평가점수 산정 이의"),
                eq("논문 실적 누락 확인 요청"), eq("인용 의견"), eq("ACCEPTED"), eq(null),
                eq(1L), eq("이의신청 의견 처리"))).thenReturn(inserted);

        ObjectionOpinionRow result = service.transition(9301L,
                new ObjectionOpinionRequest("ACCEPTED", "인용 의견", null), 1L);

        assertThat(result.decisionResult()).isEqualTo("ACCEPTED");
        verify(mapper).findLatestByObjectionId(9301L);
        verify(mapper).objectionScopeExists(9301L, 1L);
        verify(mapper).insertTransition(eq(9301L), eq("2026"), eq(2L), eq("평가점수 산정 이의"),
                eq("논문 실적 누락 확인 요청"), eq("인용 의견"), eq("ACCEPTED"), eq(null),
                eq(1L), eq("이의신청 의견 처리"));
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void transitionRejectRequiresReasonAndOpinionBeforeMapperSideEffectForReq1354Req1314() {
        ObjectionOpinionMapper mapper = org.mockito.Mockito.mock(ObjectionOpinionMapper.class);
        ObjectionOpinionService service = new ObjectionOpinionService(mapper);

        assertThatThrownBy(() -> service.transition(9301L, new ObjectionOpinionRequest("REJECTED", "", null), 1L))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insertTransition(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void transitionNeedsReviewRecordsReviewerOpinionAndKeepsSnapshotsReadonlyForReq1356Req1358() {
        ObjectionOpinionMapper mapper = org.mockito.Mockito.mock(ObjectionOpinionMapper.class);
        ObjectionOpinionService service = new ObjectionOpinionService(mapper);
        when(mapper.findLatestByObjectionId(9301L)).thenReturn(row("REJECTED"));
        when(mapper.objectionScopeExists(9301L, 1L)).thenReturn(1);
        when(mapper.insertTransition(eq(9301L), eq("2026"), eq(2L), eq("평가점수 산정 이의"),
                eq("논문 실적 누락 확인 요청"), eq("추가 검토 필요"), eq("NEEDS_REVIEW"), eq(null),
                eq(1L), eq("이의신청 의견 처리"))).thenReturn(row("NEEDS_REVIEW"));

        ObjectionOpinionRow result = service.transition(9301L,
                new ObjectionOpinionRequest("NEEDS_REVIEW", "추가 검토 필요", null), 1L);

        assertThat(result.decisionResult()).isEqualTo("NEEDS_REVIEW");
        verify(mapper).findLatestByObjectionId(9301L);
        verify(mapper).objectionScopeExists(9301L, 1L);
        verify(mapper).insertTransition(eq(9301L), eq("2026"), eq(2L), eq("평가점수 산정 이의"),
                eq("논문 실적 누락 확인 요청"), eq("추가 검토 필요"), eq("NEEDS_REVIEW"), eq(null),
                eq(1L), eq("이의신청 의견 처리"));
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void transitionOutOfScopeKeepsOpinionRowsUnchangedForReq1355() {
        ObjectionOpinionMapper mapper = org.mockito.Mockito.mock(ObjectionOpinionMapper.class);
        ObjectionOpinionService service = new ObjectionOpinionService(mapper);
        when(mapper.findLatestByObjectionId(9301L)).thenReturn(row("NEEDS_REVIEW"));
        when(mapper.objectionScopeExists(9301L, 1L)).thenReturn(0);

        assertThatThrownBy(() -> service.transition(9301L, new ObjectionOpinionRequest("ACCEPTED", "인용", null), 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이의신청 의견 처리 권한");
        verify(mapper, never()).insertTransition(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private ObjectionOpinionRow row(String decisionResult) {
        return new ObjectionOpinionRow(801L, 9301L, "2026", 2L,
                "평가점수 산정 이의", "논문 실적 누락 확인 요청", "검토자 의견",
                decisionResult, null, 1L, LocalDateTime.parse("2026-09-02T09:00:00"),
                "이의신청 의견 처리");
    }
}
