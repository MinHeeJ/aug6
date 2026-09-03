package kr.ac.knue.commonfoundation.basic45;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import org.junit.jupiter.api.Test;

class EvaluationMaterialDeletionServiceTest {
    private final EvaluationMaterialDeletionMapper mapper = org.mockito.Mockito.mock(EvaluationMaterialDeletionMapper.class);
    private final EvaluationMaterialDeletionService service = new EvaluationMaterialDeletionService(
            mapper,
            Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneId.of("Asia/Seoul")));

    @Test
    void deleteLogicallyDeletesOnlyBatchGeneratedPreviewTargetsAndRecordsResultForReq1491Req1492() {
        EvaluationBatchActionRequest request = new EvaluationBatchActionRequest(
                "2026", "EDUCATION", null, null, "B45-GENERATION-20260903-000001", "잘못 생성된 평가자료 재생성", null, null, null);
        EvaluationMaterialDeletionTarget target = new EvaluationMaterialDeletionTarget(45001L, "2026", "EDUCATION", 1L,
                43001L, "B45-GENERATION-20260903-000001", "인증", "BATCH_GENERATED", "생성 평가자료");
        when(mapper.nextDeletionSequence()).thenReturn(1L);
        when(mapper.listDeletionTargets(any())).thenReturn(List.of(target));
        when(mapper.countDeletionTargets(any())).thenReturn(1L);
        when(mapper.insertBatchRequest(any(), any(), any(), any(), any())).thenReturn(1);
        when(mapper.logicalDeleteEvaluationMaterials(any(), any(), any(), any())).thenReturn(1);
        when(mapper.insertBatchResult(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any())).thenReturn(1);

        EvaluationMaterialDeletionResult result = service.delete(request, 1L);

        assertThat(result.batchId()).isEqualTo("B45-DELETION-20260903-000001");
        assertThat(result.targetCount()).isEqualTo(1);
        assertThat(result.deletedCount()).isEqualTo(1);
        assertThat(result.excludedCount()).isZero();
        verify(mapper).insertBatchRequest(result.batchId(), "DELETION", "{\"evaluationYear\":\"2026\",\"areaCode\":\"EDUCATION\",\"generationBatchId\":\"B45-GENERATION-20260903-000001\",\"deleteReason\":\"잘못 생성된 평가자료 재생성\"}", 1L, result.requestId());
        verify(mapper).logicalDeleteEvaluationMaterials(new EvaluationMaterialDeletionSearchCriteria(0, 100, "2026", "EDUCATION", "B45-GENERATION-20260903-000001"), "잘못 생성된 평가자료 재생성", 1L, result.requestId());
        verify(mapper).insertBatchResult(result.batchId(), "DELETION", 1, 1, 0, 0, result.requestId());
    }

    @Test
    void missingDeleteReasonIsValidationErrorAndNoPersistenceChangeForReq1499() {
        EvaluationBatchActionRequest request = new EvaluationBatchActionRequest(
                "2026", "EDUCATION", null, null, "B45-GENERATION-20260903-000001", " ", null, null, null);

        assertThatThrownBy(() -> service.delete(request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("평가자료 삭제 요청이 올바르지 않습니다");

        verify(mapper, never()).insertBatchRequest(any(), any(), any(), any(), any());
        verify(mapper, never()).logicalDeleteEvaluationMaterials(any(), any(), any(), any());
    }
}
