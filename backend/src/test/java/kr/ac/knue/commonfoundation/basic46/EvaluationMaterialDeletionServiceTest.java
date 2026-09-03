package kr.ac.knue.commonfoundation.basic46;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

class EvaluationMaterialDeletionServiceTest {
    @Test
    void deleteLogicalDeletesPreviewedGeneratedMaterialsAndWritesBatchItemsForReq1477() {
        EvaluationMaterialDeletionMapper mapper = org.mockito.Mockito.mock(EvaluationMaterialDeletionMapper.class);
        EvaluationMaterialDeletionService service = new EvaluationMaterialDeletionService(mapper);
        EvaluationMaterialDeletionRequest request = request("잘못 생성된 평가자료 삭제");
        when(mapper.listDeletionCandidates(request)).thenReturn(List.of(row(460001L, "CERTIFIED", true, null)));
        when(mapper.markEvaluationMaterialDeleted(eq(460001L), eq("잘못 생성된 평가자료 삭제"), any(), eq(1L))).thenReturn(1);

        EvaluationMaterialDeletionResult result = service.delete(request, 1L);

        assertThat(result.evaluationYear()).isEqualTo("2026");
        assertThat(result.areaCode()).isEqualTo("RESEARCH_CREATION");
        assertThat(result.generationBatchId()).isEqualTo("B46-BATCH-GEN-001");
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();
        assertThat(result.excludedCount()).isZero();
        assertThat(result.requestId()).startsWith("REQ-B46-DEL-");
        verify(mapper).insertDeletionBatchJob(anyString(), eq(request), eq(1), eq(1), eq(0), eq(0), eq(1L), anyString());
        verify(mapper).markEvaluationMaterialDeleted(eq(460001L), eq("잘못 생성된 평가자료 삭제"), anyString(), eq(1L));
        verify(mapper).insertBatchJobItem(anyString(), eq("EVALUATION-MATERIAL-460001"), eq("SUCCESS"), isNull(), isNull(), isNull(), anyString(), eq(1L));
        verify(mapper).updateBatchJobCounts(anyString(), eq(1), eq(0), eq(0), eq(1L));
    }

    @Test
    void deleteRequiresPreviewTokenAndReasonBeforeMapperSideEffectsForReq1478() {
        EvaluationMaterialDeletionMapper mapper = org.mockito.Mockito.mock(EvaluationMaterialDeletionMapper.class);
        EvaluationMaterialDeletionService service = new EvaluationMaterialDeletionService(mapper);

        assertThatThrownBy(() -> service.delete(new EvaluationMaterialDeletionRequest("2026", "RESEARCH_CREATION",
                "B46-BATCH-GEN-001", "", ""), 1L))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).listDeletionCandidates(any());
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void deleteRejectsConfirmedOnlyMaterialsWithoutLogicalDeleteForReq1479() {
        EvaluationMaterialDeletionMapper mapper = org.mockito.Mockito.mock(EvaluationMaterialDeletionMapper.class);
        EvaluationMaterialDeletionService service = new EvaluationMaterialDeletionService(mapper);
        EvaluationMaterialDeletionRequest request = request("평가확정 자료 삭제 시도");
        when(mapper.listDeletionCandidates(request)).thenReturn(List.of(row(460002L, "EVALUATION_CONFIRMED", false,
                "평가확정 자료는 확정취소 후 삭제할 수 있습니다.")));

        assertThatThrownBy(() -> service.delete(request, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("확정취소");
        verify(mapper).listDeletionCandidates(request);
        verify(mapper, never()).markEvaluationMaterialDeleted(anyLong(), anyString(), anyString(), anyLong());
        verify(mapper, never()).insertDeletionBatchJob(anyString(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyLong(), anyString());
    }

    private EvaluationMaterialDeletionRequest request(String reason) {
        return new EvaluationMaterialDeletionRequest("2026", "RESEARCH_CREATION", "B46-BATCH-GEN-001", reason,
                "B46-PREVIEW-2026-RESEARCH_CREATION-B46-BATCH-GEN-001");
    }

    private EvaluationMaterialDeletionTarget row(Long materialId, String finalStatus, boolean canDelete, String excludedReason) {
        return new EvaluationMaterialDeletionTarget(materialId, "2026", "RESEARCH_CREATION", "KNUE-DEPT-COMP", 2L,
                846001L, "B46-BATCH-GEN-001", finalStatus, canDelete, excludedReason,
                LocalDateTime.parse("2026-09-03T09:00:00"));
    }
}
