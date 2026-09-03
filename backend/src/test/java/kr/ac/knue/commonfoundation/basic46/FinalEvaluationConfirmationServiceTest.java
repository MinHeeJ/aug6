package kr.ac.knue.commonfoundation.basic46;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

class FinalEvaluationConfirmationServiceTest {
    private final FinalEvaluationConfirmationMapper mapper = org.mockito.Mockito.mock(FinalEvaluationConfirmationMapper.class);
    private final FinalEvaluationConfirmationService service = new FinalEvaluationConfirmationService(mapper);

    @Test
    void confirmUpdatesOnlyCertifiedMaterialsAndStoresFinalSnapshotForReq1486Req1490() {
        FinalEvaluationTransitionRequest request = new FinalEvaluationTransitionRequest("CONFIRM", "2026", null, "최종평가 확정");
        FinalEvaluationConfirmationRow candidate = row("CERTIFIED", "SUCCESS", 2, 0, null);
        when(mapper.findLatestEvaluationYearForTarget(2L)).thenReturn("2026");
        when(mapper.findConfirmationCandidate(2L, "2026")).thenReturn(candidate);
        when(mapper.updateMaterialsStatus(eq(2L), eq("2026"), eq("CERTIFIED"), eq("EVALUATION_CONFIRMED"), any(), eq(1L))).thenReturn(2);

        FinalEvaluationTransitionResult result = service.transition(2L, request, 1L);

        assertThat(result.finalStatus()).isEqualTo("EVALUATION_CONFIRMED");
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.snapshotRef()).startsWith("B46-SNAPSHOT-");
        verify(mapper).insertFinalization(eq(2L), eq("2026"), eq("EVALUATION_CONFIRMED"), eq(1L), eq(null), eq(null),
                any(), any(), any(), any(), eq(1L));
        verify(mapper, never()).updateMaterialScoresDirectly(any(), any());
    }

    @Test
    void cancelRequiresReasonAndDoesNotChangeMaterialsForReq1489() {
        FinalEvaluationTransitionRequest request = new FinalEvaluationTransitionRequest("CANCEL", "2026", "", null);

        assertThatThrownBy(() -> service.transition(2L, request, 1L))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).updateMaterialsStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmRejectsLatestRecalculationFailureAndKeepsCertifiedStatusForReq1488() {
        FinalEvaluationTransitionRequest request = new FinalEvaluationTransitionRequest("CONFIRM", "2026", null, null);
        when(mapper.findLatestEvaluationYearForTarget(2L)).thenReturn("2026");
        when(mapper.findConfirmationCandidate(2L, "2026")).thenReturn(row("CERTIFIED", "FAILURE", 2, 0, null));

        assertThatThrownBy(() -> service.transition(2L, request, 1L))
                .isInstanceOf(ConflictException.class);
        verify(mapper, never()).insertFinalization(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(mapper, never()).updateMaterialsStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelRestoresConfirmedMaterialsToCertifiedAndPreservesPriorSnapshotForReq1487Req1491() {
        FinalEvaluationTransitionRequest request = new FinalEvaluationTransitionRequest("CANCEL", "2026", "이의신청 인용", null);
        FinalEvaluationConfirmationRow confirmed = row("EVALUATION_CONFIRMED", "SUCCESS", 2, 0, "B46-SNAPSHOT-OLD");
        when(mapper.findLatestEvaluationYearForTarget(2L)).thenReturn("2026");
        when(mapper.findConfirmationCandidate(2L, "2026")).thenReturn(confirmed);
        when(mapper.updateMaterialsStatus(eq(2L), eq("2026"), eq("EVALUATION_CONFIRMED"), eq("CERTIFIED"), any(), eq(1L))).thenReturn(2);

        FinalEvaluationTransitionResult result = service.transition(2L, request, 1L);

        assertThat(result.finalStatus()).isEqualTo("CERTIFIED");
        assertThat(result.successCount()).isEqualTo(2);
        verify(mapper).insertFinalization(eq(2L), eq("2026"), eq("CANCELLED"), eq(null), eq(1L), eq("이의신청 인용"),
                eq("B46-SNAPSHOT-OLD"), any(), any(), any(), eq(1L));
    }

    private FinalEvaluationConfirmationRow row(String finalStatus, String recalculationStatus, int materialCount,
                                               int confirmedCount, String snapshotRef) {
        return new FinalEvaluationConfirmationRow(2L, "2026", BigDecimal.valueOf(12.00), "B46-BATCH-RECALC-001",
                recalculationStatus, finalStatus, 1L, LocalDateTime.parse("2026-09-03T09:00:00"), null, null,
                null, snapshotRef, materialCount, confirmedCount);
    }
}
