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
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoreRecalculationServiceTest {
    @Mock ScoreRecalculationMapper mapper;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void recalculateRecordsNewGenerationAndUpdatesOnlySelectedTargetsForReq1501Req1517() {
        ScoreRecalculationTarget target = new ScoreRecalculationTarget(45001L, "2026", "EDUCATION", 1L, 43001L,
                "B45-GENERATION-20260903-000001", 7L, 3L, "FIXED-2026",
                new BigDecimal("8.00"), new BigDecimal("10.00"), 2, "인증", "교육 평가자료");
        when(mapper.listRecalculationTargets(any())).thenReturn(List.of(target));
        when(mapper.nextRecalculationSequence()).thenReturn(2L);
        when(mapper.updateEvaluationMaterialScore(eq(target), any(), eq(1L))).thenReturn(1);
        ScoreRecalculationService service = new ScoreRecalculationService(mapper, fixedClock);

        ScoreRecalculationResult result = service.recalculate(
                new EvaluationBatchActionRequest("2026", "EDUCATION", null, "1", null, null, "7", null, null), 1L);

        assertThat(result.batchId()).isEqualTo("B45-RECALCULATION-20260903-000002");
        assertThat(result.targetCount()).isEqualTo(1);
        assertThat(result.recalculatedCount()).isEqualTo(1);
        verify(mapper).insertScoreCalculationGeneration(eq(target), eq(result.batchId()), eq(result.requestId()), eq(1L));
        verify(mapper).updateEvaluationMaterialScore(eq(target), eq(result.requestId()), eq(1L));
        verify(mapper).insertBatchResult(eq(result.batchId()), eq("RECALCULATION"), eq(1), eq(1), eq(0), eq(0), eq(result.requestId()));
    }

    @Test
    void recalculateStoresTargetConditionAndDoesNotMutateWhenValidationFailsForReq1509Req1510() {
        ScoreRecalculationService service = new ScoreRecalculationService(mapper, fixedClock);

        assertThatThrownBy(() -> service.recalculate(
                new EvaluationBatchActionRequest("2026", null, null, null, null, null, null, null, null), 1L))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insertBatchRequest(any(), any(), any(), any(), any());
        verify(mapper, never()).updateEvaluationMaterialScore(any(), any(), any());
    }

    @Test
    void recalculateBindsOnlyProvidedOptionalFiltersForReq1508() {
        when(mapper.listRecalculationTargets(any())).thenReturn(List.of());
        when(mapper.nextRecalculationSequence()).thenReturn(3L);
        ScoreRecalculationService service = new ScoreRecalculationService(mapper, fixedClock);

        service.recalculate(new EvaluationBatchActionRequest("2026", "", null, "", null, null, "7", null, null), 1L);

        ArgumentCaptor<ScoreRecalculationSearchCriteria> captor = ArgumentCaptor.forClass(ScoreRecalculationSearchCriteria.class);
        verify(mapper).listRecalculationTargets(captor.capture());
        assertThat(captor.getValue().normalizedEvaluationYear()).isEqualTo("2026");
        assertThat(captor.getValue().normalizedFormulaVersionId()).isEqualTo(7L);
        assertThat(captor.getValue().normalizedAreaCode()).isNull();
        assertThat(captor.getValue().normalizedTargetUserId()).isNull();
    }
}
