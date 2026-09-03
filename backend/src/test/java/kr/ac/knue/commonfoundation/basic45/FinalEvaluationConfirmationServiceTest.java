package kr.ac.knue.commonfoundation.basic45;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinalEvaluationConfirmationServiceTest {
    @Mock FinalEvaluationConfirmationMapper mapper;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void confirmTransitionsCertifiedMaterialsToFinalConfirmedAndRecordsConfirmationForReq1519Req1531() {
        when(mapper.countConfirmableMaterials(1L, "2026")).thenReturn(2);
        when(mapper.nextConfirmationSequence()).thenReturn(2L);
        when(mapper.updateMaterialsStatus(1L, "2026", "인증", "평가확정", "REQ-B45-CONFIRMATION-20260903-000002", 4L)).thenReturn(2);
        FinalEvaluationConfirmationService service = new FinalEvaluationConfirmationService(mapper, fixedClock);

        FinalEvaluationConfirmationResult result = service.confirm(1L, "2026", 4L);

        assertThat(result.batchId()).isEqualTo("B45-CONFIRMATION-20260903-000002");
        assertThat(result.previousStatus()).isEqualTo("인증");
        assertThat(result.nextStatus()).isEqualTo("평가확정");
        assertThat(result.changedMaterialCount()).isEqualTo(2);
        verify(mapper).upsertConfirmation(eq(1L), eq("2026"), eq("평가확정"), eq(result.batchId()), eq(4L), eq(result.requestId()));
        verify(mapper).insertBatchResult(eq(result.batchId()), eq("CONFIRMATION"), eq(2), eq(2), eq(0), eq(0), eq(result.requestId()));
    }

    @Test
    void confirmRejectsTargetsWithoutRecalculatedCertifiedMaterialsWithoutMutationForReq1459() {
        when(mapper.countConfirmableMaterials(1L, "2026")).thenReturn(0);
        FinalEvaluationConfirmationService service = new FinalEvaluationConfirmationService(mapper, fixedClock);

        assertThatThrownBy(() -> service.confirm(1L, "2026", 4L)).isInstanceOf(ConflictException.class);
        verify(mapper, never()).insertBatchRequest(any(), any(), any(), any(), any());
        verify(mapper, never()).updateMaterialsStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelTransitionsFinalConfirmedMaterialsBackToCertifiedAndKeepsCancelReasonForReq1520Req1532() {
        when(mapper.countCancelableMaterials(1L, "2026")).thenReturn(2);
        when(mapper.nextConfirmationSequence()).thenReturn(3L);
        when(mapper.updateMaterialsStatus(1L, "2026", "평가확정", "인증", "REQ-B45-CONFIRMATION-20260903-000003", 8L)).thenReturn(2);
        FinalEvaluationConfirmationService service = new FinalEvaluationConfirmationService(mapper, fixedClock);

        FinalEvaluationConfirmationResult result = service.cancel(1L, "2026", "이의신청 인용", 8L);

        assertThat(result.batchId()).isEqualTo("B45-CONFIRMATION-20260903-000003");
        assertThat(result.previousStatus()).isEqualTo("평가확정");
        assertThat(result.nextStatus()).isEqualTo("인증");
        verify(mapper).markConfirmationCanceled(eq(1L), eq("2026"), eq(result.batchId()), eq("이의신청 인용"), eq(8L), eq(result.requestId()));
    }

    @Test
    void listUsesDynamicOptionalFiltersAndIncludesConfirmedCanceledRowsForReq1518() {
        FinalEvaluationConfirmationSearchCriteria criteria = new FinalEvaluationConfirmationSearchCriteria(0, 20, "2026", "", "", "");
        when(mapper.listConfirmations(criteria)).thenReturn(List.of(new FinalEvaluationConfirmationTarget(
                1L, "2026", "EDUCATION", "교육학과", "홍길동", "평가확정", 4L, "단과대학담당자",
                "2026-09-03T09:00:00", 8L, "점수산출감사자", "2026-09-04T09:00:00", "이의신청 인용", 2, new BigDecimal("20.00"))));
        when(mapper.countConfirmations(criteria)).thenReturn(1L);
        FinalEvaluationConfirmationService service = new FinalEvaluationConfirmationService(mapper, fixedClock);

        FinalEvaluationConfirmationListResponse response = service.list(criteria);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.confirmations().get(0).cancelReason()).isEqualTo("이의신청 인용");
        assertThat(criteria.normalizedAreaCode()).isNull();
        assertThat(criteria.normalizedTargetUserId()).isNull();
    }
}
