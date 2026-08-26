package kr.ac.knue.commonfoundation.batch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BatchDefinitionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BatchDefinitionManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean BatchDefinitionService service;

    @Test
    void saveBatchDefinitionPersistsDefinitionAndFollowUpListReturnsSameBatchId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BatchDefinitionRow saved = new BatchDefinitionRow("BATCH-EVAL-DAILY", "EVALUATION_DATA", "DAILY 02:00", 3600,
                1L, "시스템 관리자", "REQ-BATCH-001", LocalDateTime.parse("2026-08-26T10:00:00"), 1L,
                List.of("BATCH-SOURCE-SYNC"), List.of("BATCH-SCORE-CALC"), mapper.readTree("{\"year\":2026}"), "{\"year\":2026}");
        when(service.saveBatchDefinition(any(BatchDefinitionRequest.class), eq(1L), eq("REQ-BATCH-001"))).thenReturn(saved);
        when(service.listBatchDefinitions(eq(0), eq(20), any(BatchDefinitionSearchCriteria.class)))
                .thenReturn(new BatchDefinitionSearchResponse(List.of(saved), 0, 20, 1));

        mockMvc.perform(post("/api/admin/batch-definitions")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .header("X-Request-Id", "REQ-BATCH-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchId":"BATCH-EVAL-DAILY","batchType":"EVALUATION_DATA","scheduleCycle":"DAILY 02:00",
                                 "predecessorBatchIds":["BATCH-SOURCE-SYNC"],"successorBatchIds":["BATCH-SCORE-CALC"],
                                 "parameters":{"year":2026},"maxExecutionSeconds":3600,"ownerUserId":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId").value("BATCH-EVAL-DAILY"))
                .andExpect(jsonPath("$.data.scheduleCycle").value("DAILY 02:00"))
                .andExpect(jsonPath("$.data.predecessorBatchIds[0]").value("BATCH-SOURCE-SYNC"))
                .andExpect(jsonPath("$.data.successorBatchIds[0]").value("BATCH-SCORE-CALC"))
                .andExpect(jsonPath("$.data.parameters.year").value(2026))
                .andExpect(jsonPath("$.data.ownerUserId").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-BATCH-001"));

        mockMvc.perform(get("/api/admin/batch-definitions")
                        .param("batchId", "BATCH-EVAL-DAILY")
                        .param("page", "0")
                        .param("size", "20")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.definitions[0].batchId").value("BATCH-EVAL-DAILY"))
                .andExpect(jsonPath("$.data.definitions[0].scheduleCycle").value("DAILY 02:00"))
                .andExpect(jsonPath("$.data.definitions[0].parameters.year").value(2026));
    }

    @Test
    void saveBatchDefinitionRequiresBatchIdBatchTypeScheduleCycleAndOwnerUserId() throws Exception {
        mockMvc.perform(post("/api/admin/batch-definitions")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(service, never()).saveBatchDefinition(any(), any(), any());
    }

    @Test
    void saveBatchDefinitionRequiresAuthenticatedAdminSessionBeforeServiceSideEffect() throws Exception {
        mockMvc.perform(post("/api/admin/batch-definitions")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchId":"BATCH-A","batchType":"EVALUATION_DATA","scheduleCycle":"DAILY","ownerUserId":1}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveBatchDefinition(any(), any(), any());
    }

    @Test
    void serviceSavesPredecessorSuccessorDependenciesByBatchId() throws Exception {
        BatchDefinitionMapper batchMapper = mock(BatchDefinitionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        BatchDefinitionService batchService = new BatchDefinitionService(batchMapper, objectMapper);
        BatchDefinitionRequest request = validRequest("BATCH-A", "DAILY", objectMapper.readTree("{\"year\":2026}"));
        request.setPredecessorBatchIds(List.of("BATCH-PREV"));
        request.setSuccessorBatchIds(List.of("BATCH-NEXT"));
        when(batchMapper.existsUser(1L)).thenReturn(1);
        when(batchMapper.existsBatchDefinition("BATCH-PREV")).thenReturn(1);
        when(batchMapper.existsBatchDefinition("BATCH-NEXT")).thenReturn(1);
        when(batchMapper.findBatchDefinition("BATCH-A")).thenReturn(new BatchDefinitionRow("BATCH-A", "EVALUATION_DATA", "DAILY", 60,
                1L, "시스템 관리자", "REQ-1", LocalDateTime.parse("2026-08-26T10:00:00"), 1L, "{\"year\":2026}"));
        when(batchMapper.listPredecessorBatchIds("BATCH-A")).thenReturn(List.of("BATCH-PREV"));
        when(batchMapper.listSuccessorBatchIds("BATCH-A")).thenReturn(List.of("BATCH-NEXT"));

        batchService.saveBatchDefinition(request, 1L, "REQ-1");

        verify(batchMapper).insertDependency("BATCH-PREV", "BATCH-A", 1L, "REQ-1");
        verify(batchMapper).insertDependency("BATCH-A", "BATCH-NEXT", 1L, "REQ-1");
    }

    @Test
    void serviceChangesOnlySelectedBatchScheduleAndParameters() throws Exception {
        BatchDefinitionMapper batchMapper = mock(BatchDefinitionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        BatchDefinitionService batchService = new BatchDefinitionService(batchMapper, objectMapper);
        BatchDefinitionRequest request = validRequest("BATCH-A", "WEEKLY", objectMapper.readTree("{\"term\":\"2026-1\"}"));
        when(batchMapper.existsUser(1L)).thenReturn(1);
        when(batchMapper.findBatchDefinition("BATCH-A")).thenReturn(new BatchDefinitionRow("BATCH-A", "EVALUATION_DATA", "WEEKLY", 60,
                1L, "시스템 관리자", "REQ-2", LocalDateTime.parse("2026-08-26T10:00:00"), 1L, "{\"term\":\"2026-1\"}"));
        when(batchMapper.listPredecessorBatchIds("BATCH-A")).thenReturn(List.of());
        when(batchMapper.listSuccessorBatchIds("BATCH-A")).thenReturn(List.of());

        batchService.saveBatchDefinition(request, 1L, "REQ-2");

        verify(batchMapper).upsertBatchDefinition(eq(request), eq(1L), eq("REQ-2"));
        verify(batchMapper).upsertBatchParameters(eq("BATCH-A"), eq("{\"term\":\"2026-1\"}"), eq(1L), eq("REQ-2"));
        verify(batchMapper, never()).upsertBatchParameters(eq("BATCH-B"), any(), any(), any());
    }

    @Test
    void serviceRejectsInvalidDependencyBeforeWritingDefinition() {
        BatchDefinitionMapper batchMapper = mock(BatchDefinitionMapper.class);
        BatchDefinitionService batchService = new BatchDefinitionService(batchMapper, new ObjectMapper());
        BatchDefinitionRequest request = validRequest("BATCH-A", "DAILY", null);
        request.setSuccessorBatchIds(List.of("BATCH-A"));
        when(batchMapper.existsUser(1L)).thenReturn(1);

        assertThatThrownBy(() -> batchService.saveBatchDefinition(request, 1L, "REQ-3"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("배치 정의");
        verify(batchMapper, never()).upsertBatchDefinition(any(), any(), any());
    }

    private BatchDefinitionRequest validRequest(String batchId, String scheduleCycle, com.fasterxml.jackson.databind.JsonNode parameters) {
        BatchDefinitionRequest request = new BatchDefinitionRequest();
        request.setBatchId(batchId);
        request.setBatchType("EVALUATION_DATA");
        request.setScheduleCycle(scheduleCycle);
        request.setOwnerUserId(1L);
        request.setMaxExecutionSeconds(60);
        request.setParameters(parameters);
        return request;
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }
}
