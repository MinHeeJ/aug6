package kr.ac.knue.commonfoundation.basic45;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationBatchResultServiceTest {
    @Mock EvaluationBatchResultMapper mapper;

    @Test
    void listUsesDynamicFiltersAndReturnsOriginalCountsWithoutMutationForReq1539Req1548() {
        EvaluationBatchResultSearchCriteria criteria = new EvaluationBatchResultSearchCriteria(
                0, 20, "B45-GENERATION-20260903-000001", "GENERATION", "EDUCATION");
        when(mapper.listResults(criteria)).thenReturn(List.of(new EvaluationBatchResultRow(
                "B45-GENERATION-20260903-000001", "GENERATION", "생성", "COMPLETED",
                "{\"evaluationYear\":\"2026\",\"areaCode\":\"EDUCATION\"}",
                3, 1, 1, 1, "REQ-B45-SEED-003", "2026-09-03T09:00:00", "2026-09-03T09:01:00")));
        when(mapper.countResults(criteria)).thenReturn(1L);
        EvaluationBatchResultService service = new EvaluationBatchResultService(mapper);

        EvaluationBatchResultListResponse response = service.list(criteria);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.results().get(0).failureCount()).isEqualTo(1);
        assertThat(response.results().get(0).excludedCount()).isEqualTo(1);
        assertThat(criteria.normalizedTargetCondition()).isEqualTo("EDUCATION");
        verify(mapper, never()).listErrors("B45-GENERATION-20260903-000001", 0, 20);
    }

    @Test
    void listErrorsReturnsFailedTargetIdentityAndKeepsSourceDataReadOnlyForReq1546Req1549() {
        when(mapper.listErrors("B45-GENERATION-20260903-000001", 0, 20)).thenReturn(List.of(
                new EvaluationBatchResultErrorRow(
                        "B45-GENERATION-20260903-000001", "ACH-43002", "연구업적 미인증",
                        "SOURCE_STATUS_NOT_CERTIFIED", "인증 상태 원천만 평가자료로 생성할 수 있습니다.", "sourceStatus=제출")));
        when(mapper.countErrors("B45-GENERATION-20260903-000001")).thenReturn(1L);
        EvaluationBatchResultService service = new EvaluationBatchResultService(mapper);

        EvaluationBatchResultErrorListResponse response = service.listErrors(" B45-GENERATION-20260903-000001 ", 0, 20);

        assertThat(response.batchId()).isEqualTo("B45-GENERATION-20260903-000001");
        assertThat(response.errors().get(0).targetKey()).isEqualTo("ACH-43002");
        assertThat(response.errors().get(0).errorCode()).isEqualTo("SOURCE_STATUS_NOT_CERTIFIED");
        verify(mapper, never()).listResults(new EvaluationBatchResultSearchCriteria(0, 20, null, null, null));
    }
}
