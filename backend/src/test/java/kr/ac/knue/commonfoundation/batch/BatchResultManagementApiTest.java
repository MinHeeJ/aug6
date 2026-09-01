package kr.ac.knue.commonfoundation.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BatchResultController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BatchResultManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean BatchResultService service;

    @Test
    void listBatchResultsReturnsStartedEndedCountsAndElapsedMillis() throws Exception {
        BatchResultRow row = new BatchResultRow("EXEC-SUCCESS-001", "SEED-BATCH-DEF-001", "EVALUATION_DATA",
                "COMPLETED", LocalDateTime.parse("2026-08-26T02:00:00"),
                LocalDateTime.parse("2026-08-26T02:05:30"), 120, 118, 1, 1, 330000L, true);
        when(service.listBatchResults(eq(0), eq(20), any(BatchResultSearchCriteria.class)))
                .thenReturn(new BatchResultSearchResponse(List.of(row), 0, 20, 1));

        mockMvc.perform(get("/api/admin/batch-results")
                        .param("page", "0")
                        .param("size", "20")
                        .param("batchId", "SEED-BATCH-DEF-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].executionId").value("EXEC-SUCCESS-001"))
                .andExpect(jsonPath("$.data.results[0].batchId").value("SEED-BATCH-DEF-001"))
                .andExpect(jsonPath("$.data.results[0].executionStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.results[0].startedAt").value("2026-08-26T02:00:00"))
                .andExpect(jsonPath("$.data.results[0].endedAt").value("2026-08-26T02:05:30"))
                .andExpect(jsonPath("$.data.results[0].totalCount").value(120))
                .andExpect(jsonPath("$.data.results[0].successCount").value(118))
                .andExpect(jsonPath("$.data.results[0].failureCount").value(1))
                .andExpect(jsonPath("$.data.results[0].excludedCount").value(1))
                .andExpect(jsonPath("$.data.results[0].elapsedMillis").value(330000))
                .andExpect(jsonPath("$.data.results[0].hasLog").value(true));
    }

    @Test
    void basic37ListBatchResultsContractReturnsApiResponseForSeedExecutionWithoutDtoMappingLeak() throws Exception {
        BatchResultRow row = new BatchResultRow("BASIC37-SEED-BATCH-001", "BASIC37-SEED-BATCH-001", "FACULTY_PROFILE_SYNC",
                "COMPLETED", LocalDateTime.parse("2026-08-27T01:00:00"),
                LocalDateTime.parse("2026-08-27T01:05:00"), 50, 49, 1, 0, 300000L, true);
        when(service.listBatchResults(eq(0), eq(20), any(BatchResultSearchCriteria.class)))
                .thenReturn(new BatchResultSearchResponse(List.of(row), 0, 20, 1));

        mockMvc.perform(get("/api/admin/batch-results")
                        .param("page", "0")
                        .param("size", "20")
                        .param("executionId", "BASIC37-SEED-BATCH-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].executionId").value("BASIC37-SEED-BATCH-001"))
                .andExpect(jsonPath("$.data.results[0].batchType").value("FACULTY_PROFILE_SYNC"))
                .andExpect(jsonPath("$.data.results[0].totalCount").value(50))
                .andExpect(jsonPath("$.data.results[0].elapsedMillis").value(300000))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void getBatchResultLogReturnsExecutionLinkedLogWithoutMutationEndpoint() throws Exception {
        when(service.getBatchResultLog("EXEC-FAILED-001"))
                .thenReturn(new BatchResultLogResponse("EXEC-FAILED-001", "logs/batch/EXEC-FAILED-001.log"));

        mockMvc.perform(get("/api/admin/batch-results/{executionId}/log", "EXEC-FAILED-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executionId").value("EXEC-FAILED-001"))
                .andExpect(jsonPath("$.data.logFileRef").value("logs/batch/EXEC-FAILED-001.log"));

        mockMvc.perform(put("/api/admin/batch-results/{executionId}/log", "EXEC-FAILED-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logFileRef\":\"changed\"}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/admin/batch-results/{executionId}/log", "EXEC-FAILED-001"))
                .andExpect(status().isMethodNotAllowed());
        assertThat(List.of(BatchResultService.class.getDeclaredMethods()).stream()
                .map(method -> method.getName()).toList())
                .doesNotContain("replaceBatchResultLog", "deleteBatchResultLog", "updateBatchResult");
    }

    @Test
    void basic37GetBatchResultLogContractReturnsReadonlyLogRefForSelectedExecution() throws Exception {
        when(service.getBatchResultLog("BASIC37-SEED-BATCH-001"))
                .thenReturn(new BatchResultLogResponse("BASIC37-SEED-BATCH-001", "logs/batch/BASIC37-SEED-BATCH-001.log"));

        mockMvc.perform(get("/api/admin/batch-results/{executionId}/log", "BASIC37-SEED-BATCH-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.executionId").value("BASIC37-SEED-BATCH-001"))
                .andExpect(jsonPath("$.data.logFileRef").value("logs/batch/BASIC37-SEED-BATCH-001.log"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void resultInquiryDoesNotExposeRerunOrResultMutationApi() throws Exception {
        mockMvc.perform(post("/api/admin/batch-results/{executionId}/rerun", "EXEC-FAILED-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"화면 결과 조회에서 재실행 금지\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/admin/batch-results/{executionId}", "EXEC-FAILED-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"failureCount\":0}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/admin/batch-results/{executionId}", "EXEC-FAILED-001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void serviceQueriesLogsByExecutionIdAndProvidesNoWriteMethodSideEffect() {
        BatchResultMapper mapper = mock(BatchResultMapper.class);
        BatchResultService resultService = new BatchResultService(mapper);
        when(mapper.findBatchResultLog("EXEC-LOG-001"))
                .thenReturn(new BatchResultLogResponse("EXEC-LOG-001", "logs/batch/EXEC-LOG-001.log"));

        BatchResultLogResponse log = resultService.getBatchResultLog("EXEC-LOG-001");

        assertThat(log.executionId()).isEqualTo("EXEC-LOG-001");
        assertThat(log.logFileRef()).isEqualTo("logs/batch/EXEC-LOG-001.log");
        verify(mapper).findBatchResultLog("EXEC-LOG-001");
        assertThat(List.of(BatchResultMapper.class.getDeclaredMethods()).stream()
                .map(method -> method.getName()).toList())
                .doesNotContain("updateBatchResultLog", "deleteBatchResultLog", "updateBatchResult");
    }
}