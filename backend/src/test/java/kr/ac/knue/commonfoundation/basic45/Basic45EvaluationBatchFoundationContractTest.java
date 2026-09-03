package kr.ac.knue.commonfoundation.basic45;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.junit.jupiter.api.Test;

class Basic45EvaluationBatchFoundationContractTest {
    @Test
    void businessBatchApiUsesCommonEnvelopeWithRequestIdTraceMetadata() {
        ApiResponse<Basic45EvaluationBatchFoundationContract.BatchSubmission> response =
                Basic45EvaluationBatchFoundationContract.okWithRequestId(
                        new Basic45EvaluationBatchFoundationContract.BatchSubmission(
                                "B45-GENERATION-20260903-000001",
                                "REQ-B45-FOUNDATION-001"),
                        "REQ-B45-FOUNDATION-001");

        assertThat(response.success()).isTrue();
        assertThat(response.error()).isNull();
        assertThat(response.meta()).containsEntry("requestId", "REQ-B45-FOUNDATION-001");
        assertThat(response.meta()).containsKeys("timestamp", "traceId");
        assertThat(response.data().batchId()).startsWith("B45-GENERATION-20260903-");
        assertThat(response.data().requestId()).isEqualTo("REQ-B45-FOUNDATION-001");
    }

    @Test
    void batchIdNamingKeepsJobTypeDateAndSixDigitSequence() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-03T10:15:30Z"), ZoneId.of("Asia/Seoul"));

        String batchId = Basic45EvaluationBatchFoundationContract.batchId("RECALCULATION", clock, 42L);

        assertThat(batchId).isEqualTo("B45-RECALCULATION-20260903-000042");
    }

    @Test
    void batchApiPathsMapToExistingReactShellGuardRoutes() {
        assertThat(Basic45EvaluationBatchFoundationContract.uiRouteForApiPath(
                "/api/business/evaluation-material-generations/preview"))
                .isEqualTo("/admin/evaluation-material-generations");
        assertThat(Basic45EvaluationBatchFoundationContract.uiRouteForApiPath(
                "/api/business/evaluation-material-deletions/preview"))
                .isEqualTo("/admin/evaluation-material-deletions");
        assertThat(Basic45EvaluationBatchFoundationContract.uiRouteForApiPath(
                "/api/business/score-recalculations"))
                .isEqualTo("/admin/score-recalculations");
        assertThat(Basic45EvaluationBatchFoundationContract.uiRouteForApiPath(
                "/api/business/final-evaluation-confirmations/1/cancel"))
                .isEqualTo("/admin/final-evaluation-confirmations");
        assertThat(Basic45EvaluationBatchFoundationContract.uiRouteForApiPath(
                "/api/business/evaluation-batch-results/B45-GENERATION-20260903-000001/errors"))
                .isEqualTo("/admin/evaluation-batch-results");
    }
}
