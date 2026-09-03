package kr.ac.knue.commonfoundation.basic45;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvaluationMaterialGenerationServiceTest {
    private final EvaluationMaterialGenerationMapper mapper = org.mockito.Mockito.mock(EvaluationMaterialGenerationMapper.class);
    private final EvaluationMaterialGenerationService service = new EvaluationMaterialGenerationService(
            mapper,
            Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneId.of("Asia/Seoul")));

    @Test
    void createRecordsCriteriaCountsResultAndDoesNotUpdateSourceRowsForReq1463Req1473Req1480() {
        EvaluationMaterialGenerationTarget target = new EvaluationMaterialGenerationTarget(9101L, "2026", "EDUCATION", "ORG-001", 2L,
                "인증", "논문", "인증 원천 실적", "미생성");
        when(mapper.nextGenerationSequence()).thenReturn(2L);
        when(mapper.listGenerationTargets(any())).thenReturn(List.of(target));
        when(mapper.countGenerationTargets(any())).thenReturn(1L);
        when(mapper.insertBatchRequest(any(), any(), any(), any(), any())).thenReturn(1);
        when(mapper.insertEvaluationMaterial(any(), any(), any(), any())).thenReturn(1);
        when(mapper.insertBatchResult(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any())).thenReturn(1);

        EvaluationMaterialGenerationResult result = service.create(
                new EvaluationBatchActionRequest("2026", "EDUCATION", "ORG-001", "2", null, null, null, null, null), 1L);

        assertThat(result.batchId()).isEqualTo("B45-GENERATION-20260903-000002");
        assertThat(result.targetCount()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.excludedCount()).isZero();
        verify(mapper).insertBatchRequest(result.batchId(), "GENERATION", "{\"evaluationYear\":\"2026\",\"areaCode\":\"EDUCATION\",\"organizationCode\":\"ORG-001\",\"targetUserId\":\"2\"}", 1L, result.requestId());
        verify(mapper).insertEvaluationMaterial(target, result.batchId(), result.requestId(), 1L);
        verify(mapper).insertBatchResult(result.batchId(), "GENERATION", 1, 1, 0, 0, result.requestId());
    }
}
