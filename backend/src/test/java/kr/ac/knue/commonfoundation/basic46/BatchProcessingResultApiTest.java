package kr.ac.knue.commonfoundation.basic46;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BatchProcessingResultController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BatchProcessingResultApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean BatchProcessingResultService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listBatchProcessingResultsReturnsCountsAndSearchFiltersForReq1493Req1495() throws Exception {
        when(service.list(new BatchProcessingResultSearchCriteria(0, 20, "GENERATION", "2026 RESEARCH_CREATION", "B46-BATCH-GEN-001")))
                .thenReturn(new BatchProcessingResultSearchResponse(
                        List.of(resultRow()), 0, 20, 1));

        mockMvc.perform(get("/api/business/batch-processing-results")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("batchType", "GENERATION")
                        .param("targetCondition", "2026 RESEARCH_CREATION")
                        .param("batchId", "B46-BATCH-GEN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].batchId").value("B46-BATCH-GEN-001"))
                .andExpect(jsonPath("$.data.results[0].batchType").value("GENERATION"))
                .andExpect(jsonPath("$.data.results[0].totalCount").value(3))
                .andExpect(jsonPath("$.data.results[0].successCount").value(1))
                .andExpect(jsonPath("$.data.results[0].failureCount").value(1))
                .andExpect(jsonPath("$.data.results[0].excludedCount").value(1))
                .andExpect(jsonPath("$.data.results[0].targetConditionSummary").value("평가연도 2026 / 영역 RESEARCH_CREATION / 조직 KNUE-DEPT-COMP / 대상자 52"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/business/batch-processing-results")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new BatchProcessingResultSearchCriteria(0, 20, null, null, null));
    }

    @Test
    void listBatchProcessingResultErrorsReturnsOnlyFailureAndExcludedDetailsForReq1494Req1496() throws Exception {
        when(service.listErrors("B46-BATCH-GEN-001"))
                .thenReturn(List.of(
                        new BatchProcessingResultErrorRow(10L, "B46-BATCH-GEN-001", "SOURCE-ACHIEVEMENT-846198", "FAILURE", "B46_VALIDATION_ERROR", "필수 평가영역을 확인할 수 없습니다.", null, LocalDateTime.parse("2026-09-03T09:10:00")),
                        new BatchProcessingResultErrorRow(11L, "B46-BATCH-GEN-001", "SOURCE-ACHIEVEMENT-846099", "EXCLUDED", null, null, "인증 미만 원천 실적 제외", LocalDateTime.parse("2026-09-03T09:11:00"))));

        mockMvc.perform(get("/api/business/batch-processing-results/B46-BATCH-GEN-001/errors")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].batchId").value("B46-BATCH-GEN-001"))
                .andExpect(jsonPath("$.data[0].targetRef").value("SOURCE-ACHIEVEMENT-846198"))
                .andExpect(jsonPath("$.data[0].resultStatus").value("FAILURE"))
                .andExpect(jsonPath("$.data[0].errorCode").value("B46_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("EXCLUDED"))
                .andExpect(jsonPath("$.data[1].excludedReason").value("인증 미만 원천 실적 제외"));
    }

    @Test
    void listBatchProcessingResultErrorsRejectsBlankBatchIdAndNeverExecutesBatchForReq1497() throws Exception {
        mockMvc.perform(get("/api/business/batch-processing-results/%20/errors")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'batchId')]").exists());
        verify(service, never()).listErrors(" ");
        verify(service, never()).createOrRerun(any());
    }

    private BatchProcessingResultRow resultRow() {
        return new BatchProcessingResultRow("B46-BATCH-GEN-001", "GENERATION", "2026", "RESEARCH_CREATION",
                "KNUE-DEPT-COMP", 52L, "평가연도 2026 / 영역 RESEARCH_CREATION / 조직 KNUE-DEPT-COMP / 대상자 52",
                3, 1, 1, 1, "COMPLETED", 1L, LocalDateTime.parse("2026-09-03T09:00:00"), "REQ-B46-SEED-GEN-001");
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
