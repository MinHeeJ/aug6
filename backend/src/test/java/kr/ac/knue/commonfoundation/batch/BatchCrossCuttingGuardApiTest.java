package kr.ac.knue.commonfoundation.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({BatchDefinitionController.class, BatchExecutionController.class, BatchResultController.class, BatchRetryController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BatchCrossCuttingGuardApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean BatchDefinitionService definitionService;
    @MockBean BatchExecutionService executionService;
    @MockBean BatchResultService resultService;
    @MockBean BatchRetryService retryService;

    @Test
    void batchListsDefaultToTwentyItemsAcrossFourScreens() throws Exception {
        when(definitionService.listBatchDefinitions(eq(0), eq(20), any()))
                .thenReturn(new BatchDefinitionSearchResponse(List.of(), 0, 20, 0));
        when(executionService.listBatchExecutions(eq(0), eq(20), any()))
                .thenReturn(new BatchExecutionSearchResponse(List.of(), 0, 20, 0));
        when(resultService.listBatchResults(eq(0), eq(20), any()))
                .thenReturn(new BatchResultSearchResponse(List.of(), 0, 20, 0));
        when(retryService.listBatchRetryTargets(eq(0), eq(20), any()))
                .thenReturn(new BatchRetryTargetSearchResponse(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/admin/batch-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
        mockMvc.perform(get("/api/admin/batch-executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
        mockMvc.perform(get("/api/admin/batch-results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
        mockMvc.perform(get("/api/admin/batch-retries/targets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void batchListsAcceptOnlyReviewerApprovedTwentyFiftyHundredSizes() throws Exception {
        when(definitionService.listBatchDefinitions(eq(0), eq(50), any()))
                .thenReturn(new BatchDefinitionSearchResponse(List.of(), 0, 50, 0));
        when(executionService.listBatchExecutions(eq(0), eq(100), any()))
                .thenReturn(new BatchExecutionSearchResponse(List.of(), 0, 100, 0));
        when(resultService.listBatchResults(eq(0), eq(50), any()))
                .thenReturn(new BatchResultSearchResponse(List.of(), 0, 50, 0));
        when(retryService.listBatchRetryTargets(eq(0), eq(100), any()))
                .thenReturn(new BatchRetryTargetSearchResponse(List.of(), 0, 100, 0));

        mockMvc.perform(get("/api/admin/batch-definitions").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(50));
        mockMvc.perform(get("/api/admin/batch-executions").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100));
        mockMvc.perform(get("/api/admin/batch-results").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(50));
        mockMvc.perform(get("/api/admin/batch-retries/targets").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100));

        verify(definitionService).listBatchDefinitions(eq(0), eq(50), any(BatchDefinitionSearchCriteria.class));
        verify(executionService).listBatchExecutions(eq(0), eq(100), any(BatchExecutionSearchCriteria.class));
        verify(resultService).listBatchResults(eq(0), eq(50), any(BatchResultSearchCriteria.class));
        verify(retryService).listBatchRetryTargets(eq(0), eq(100), any(BatchRetryTargetSearchCriteria.class));
    }
}
