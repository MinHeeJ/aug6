package kr.ac.knue.commonfoundation.batch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BatchExecutionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BatchExecutionManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean BatchExecutionService service;

    @Test
    void createBatchExecutionPersistsParametersReasonAndFollowUpListReturnsRunningExecution() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BatchExecutionRow running = execution("BEX-MANUAL-001", "BATCH-EVAL-DAILY", "RUNNING", "MANUAL_RUN",
                "평가자료 수동 생성", null, "REQ-BEX-001", "{\"year\":2026}").withParameters(mapper.readTree("{\"year\":2026}"));
        when(service.createBatchExecution(any(BatchExecutionRequest.class), eq(1L), eq("REQ-BEX-001"))).thenReturn(running);
        when(service.listBatchExecutions(eq(0), eq(20), any(BatchExecutionSearchCriteria.class)))
                .thenReturn(new BatchExecutionSearchResponse(List.of(running), 0, 20, 1));

        mockMvc.perform(post("/api/admin/batch-executions")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .header("X-Request-Id", "REQ-BEX-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchId":"BATCH-EVAL-DAILY","parameters":{"year":2026},"reason":"평가자료 수동 생성"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId").value("BATCH-EVAL-DAILY"))
                .andExpect(jsonPath("$.data.executionStatus").value("RUNNING"))
                .andExpect(jsonPath("$.data.processType").value("MANUAL_RUN"))
                .andExpect(jsonPath("$.data.reason").value("평가자료 수동 생성"))
                .andExpect(jsonPath("$.data.parameters.year").value(2026))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-BEX-001"));

        mockMvc.perform(get("/api/admin/batch-executions")
                        .param("executionStatus", "RUNNING")
                        .param("page", "0")
                        .param("size", "20")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executions[0].executionStatus").value("RUNNING"))
                .andExpect(jsonPath("$.data.executions[0].parameters.year").value(2026));
    }

    @Test
    void updateBatchExecutionStatusStopsRunningExecutionWithReasonAndOperator() throws Exception {
        BatchExecutionRow stopped = execution("SEED-BATCH-EXEC-001", "SEED-BATCH-DEF-001", "STOPPED", "STOP",
                "운영자 요청 중지", null, "REQ-BEX-STOP", "{}");
        when(service.updateBatchExecutionStatus(eq("SEED-BATCH-EXEC-001"), any(BatchExecutionStatusRequest.class), eq(1L), eq("REQ-BEX-STOP")))
                .thenReturn(stopped);

        mockMvc.perform(patch("/api/admin/batch-executions/SEED-BATCH-EXEC-001/status")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .header("X-Request-Id", "REQ-BEX-STOP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"STOPPED\",\"reason\":\"운영자 요청 중지\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executionId").value("SEED-BATCH-EXEC-001"))
                .andExpect(jsonPath("$.data.executionStatus").value("STOPPED"))
                .andExpect(jsonPath("$.data.reason").value("운영자 요청 중지"))
                .andExpect(jsonPath("$.data.operatorUserId").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-BEX-STOP"));
    }

    @Test
    void createBatchRerunCreatesNewRunningExecutionLinkedToOriginalExecutionId() throws Exception {
        BatchExecutionRow rerun = execution("BEX-RERUN-001", "SEED-BATCH-DEF-001", "RUNNING", "RERUN",
                "장애 조치 후 재실행", "SEED-BATCH-EXEC-001", "REQ-BEX-RERUN", "{\"year\":2026}");
        when(service.createBatchRerun(eq("SEED-BATCH-EXEC-001"), any(BatchExecutionRequest.class), eq(1L), eq("REQ-BEX-RERUN")))
                .thenReturn(rerun.withParameters(new ObjectMapper().readTree("{\"year\":2026}")));

        mockMvc.perform(post("/api/admin/batch-executions/SEED-BATCH-EXEC-001/rerun")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .header("X-Request-Id", "REQ-BEX-RERUN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parameters\":{\"year\":2026},\"reason\":\"장애 조치 후 재실행\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processType").value("RERUN"))
                .andExpect(jsonPath("$.data.executionStatus").value("RUNNING"))
                .andExpect(jsonPath("$.data.originalExecutionId").value("SEED-BATCH-EXEC-001"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-BEX-RERUN"));
    }

    @Test
    void mutatingExecutionActionsRejectNonR09BeforeServiceSideEffect() throws Exception {
        mockMvc.perform(post("/api/admin/batch-executions")
                        .requestAttr("currentUser", new CurrentUser(2L, "operator", "E0002", "일반 사용자", List.of("R01"), List.of()))
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchId\":\"SEED-BATCH-DEF-001\",\"reason\":\"권한 없음 검증\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).createBatchExecution(any(), any(), any());
    }

    @Test
    void createBatchExecutionRequiresReasonBeforeServiceSideEffect() throws Exception {
        mockMvc.perform(post("/api/admin/batch-executions")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchId\":\"SEED-BATCH-DEF-001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(service, never()).createBatchExecution(any(), any(), any());
    }

    @Test
    void serviceRejectsStoppingNonRunningExecutionWithoutStateChange() {
        BatchExecutionMapper mapper = mock(BatchExecutionMapper.class);
        BatchExecutionService executionService = new BatchExecutionService(mapper, new ObjectMapper());
        BatchExecutionStatusRequest request = new BatchExecutionStatusRequest();
        request.setTargetStatus("STOPPED");
        request.setReason("완료 건 중지 시도");
        when(mapper.findBatchExecution("BEX-DONE")).thenReturn(execution("BEX-DONE", "BATCH-A", "COMPLETED",
                "MANUAL_RUN", "완료", null, "REQ-DONE", "{}"));

        assertThatThrownBy(() -> executionService.updateBatchExecutionStatus("BEX-DONE", request, 1L, "REQ-STOP"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RUNNING");
        verify(mapper, never()).stopBatchExecution(any(), any(), any(), any());
    }

    private BatchExecutionRow execution(String executionId, String batchId, String status, String processType, String reason,
            String originalExecutionId, String requestId, String parameterJson) {
        return new BatchExecutionRow(executionId, batchId, "EVALUATION_DATA", status, processType, reason, 1L,
                "시스템 관리자", originalExecutionId, requestId, LocalDateTime.parse("2026-08-26T10:00:00"),
                LocalDateTime.parse("2026-08-26T10:00:00"), parameterJson);
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }
}
