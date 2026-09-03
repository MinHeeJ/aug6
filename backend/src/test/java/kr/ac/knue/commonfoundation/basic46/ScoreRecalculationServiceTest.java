package kr.ac.knue.commonfoundation.basic46;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ScoreRecalculationServiceTest {
    private final ScoreRecalculationMapper mapper = org.mockito.Mockito.mock(ScoreRecalculationMapper.class);
    private final ScoreRecalculationService service = new ScoreRecalculationService(mapper);

    @Test
    void createPreservesPreviousGenerationAndRecordsNewGenerationOnlyForEligibleMaterialsForReq1481Req1483() {
        ScoreRecalculationRequest request = new ScoreRecalculationRequest(
                "2026", "RESEARCH_CREATION", null, "320001", "선택 산식버전 재계산");
        ScoreFormulaSnapshot formula = new ScoreFormulaSnapshot(320001L, "CAP", BigDecimal.ZERO, BigDecimal.valueOf(11.00));
        ScoreRecalculationCandidate eligible = new ScoreRecalculationCandidate(460001L, "2026", "RESEARCH_CREATION",
                "KNUE-DEPT-COMP", 2L, 846001L, "CERTIFIED", "N", BigDecimal.valueOf(12.00),
                BigDecimal.valueOf(10.00), "SOLE_AUTHOR", BigDecimal.ONE, "N", 2);
        ScoreRecalculationCandidate confirmed = new ScoreRecalculationCandidate(460003L, "2026", "RESEARCH_CREATION",
                "KNUE-DEPT-COMP", 2L, 846003L, "EVALUATION_CONFIRMED", "N", BigDecimal.valueOf(20.00),
                BigDecimal.valueOf(20.00), "SOLE_AUTHOR", BigDecimal.ONE, "N", 1);
        when(mapper.findFormulaSnapshot(320001L, "2026")).thenReturn(formula);
        when(mapper.listRecalculationCandidates(request)).thenReturn(List.of(eligible, confirmed));
        when(mapper.insertScoreGeneration(eq(eligible), eq(320001L), eq(BigDecimal.valueOf(10.00)),
                eq(BigDecimal.valueOf(11.00).setScale(2)), eq("선택 산식버전 재계산"), any(), any(), eq(1L)))
                .thenReturn(1);

        ScoreRecalculationResult result = service.create(request, 1L);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.excludedCount()).isEqualTo(1);
        assertThat(result.recalculationBatchId()).startsWith("B46-RECALC-");
        verify(mapper).insertScoreGeneration(eq(eligible), eq(320001L), eq(BigDecimal.valueOf(10.00)),
                eq(BigDecimal.valueOf(11.00).setScale(2)), eq("선택 산식버전 재계산"), any(), any(), eq(1L));
        verify(mapper, never()).insertScoreGeneration(eq(confirmed), any(), any(), any(), any(), any(), any(), any());
        verify(mapper).insertBatchJobItem(any(), eq("EVALUATION-MATERIAL-460003"), eq("EXCLUDED"), eq(null), eq(null),
                eq("평가확정 자료는 확정취소 후 재계산할 수 있습니다."), any(), eq(1L));
    }

    @Test
    void createRejectsMissingFormulaVersionAndDoesNotTouchFormulaOrSourceForReq1484() {
        ScoreRecalculationRequest request = new ScoreRecalculationRequest("2026", "RESEARCH_CREATION", null, "", "");

        assertThatThrownBy(() -> service.create(request, 1L))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).findFormulaSnapshot(any(), any());
        verify(mapper, never()).listRecalculationCandidates(any());
    }

    @Test
    void createRejectsUnknownFormulaVersionBeforeWritingBatchForReq1480() {
        ScoreRecalculationRequest request = new ScoreRecalculationRequest(
                "2026", "RESEARCH_CREATION", 2L, "320099", "허용된 산식버전 확인");
        when(mapper.findFormulaSnapshot(320099L, "2026")).thenReturn(null);

        assertThatThrownBy(() -> service.create(request, 1L))
                .isInstanceOf(ConflictException.class);
        verify(mapper, never()).insertRecalculationBatchJob(any(), any(), any(), any(Integer.class),
                any(Integer.class), any(Integer.class), any(Integer.class), any(), any());
    }

    @Test
    void createRecordsSelectionReasonInBatchConditionAndGenerationForReq1482() {
        ScoreRecalculationRequest request = new ScoreRecalculationRequest(
                "2026", "RESEARCH_CREATION", 2L, "320001", "평가년도 기본 규정버전과 비교 재계산");
        ScoreRecalculationCandidate eligible = new ScoreRecalculationCandidate(460002L, "2026", "RESEARCH_CREATION",
                "KNUE-DEPT-COMP", 2L, 846002L, "CERTIFIED", "N", BigDecimal.valueOf(5.00),
                BigDecimal.ZERO, "CO_AUTHOR", BigDecimal.valueOf(0.5), "Y", 3);
        when(mapper.findFormulaSnapshot(320001L, "2026"))
                .thenReturn(new ScoreFormulaSnapshot(320001L, "DISTRIBUTION_RATE", BigDecimal.ZERO, null));
        when(mapper.listRecalculationCandidates(request)).thenReturn(List.of(eligible));
        when(mapper.insertScoreGeneration(eq(eligible), eq(320001L), eq(BigDecimal.ZERO),
                eq(BigDecimal.valueOf(2.50).setScale(2)), eq("평가년도 기본 규정버전과 비교 재계산"), any(), any(), eq(1L)))
                .thenReturn(1);

        service.create(request, 1L);

        ArgumentCaptor<ScoreRecalculationRequest> requestCaptor = ArgumentCaptor.forClass(ScoreRecalculationRequest.class);
        verify(mapper).insertRecalculationBatchJob(any(), requestCaptor.capture(), eq(320001L), eq(1), eq(1), eq(0), eq(0), eq(1L), any());
        assertThat(requestCaptor.getValue().selectionReason()).isEqualTo("평가년도 기본 규정버전과 비교 재계산");
    }
}
