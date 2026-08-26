package kr.ac.knue.commonfoundation.batch;

import static org.assertj.core.api.Assertions.assertThat;
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

@WebMvcTest(BatchRetryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BatchRetryManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean BatchRetryService service;

    @Test
    void listBatchRetryTargetsReturnsOnlyFailedBatchAndItemTargets() throws Exception {
        BatchRetryTargetRow failedTarget = new BatchRetryTargetRow("EXEC-FAILED-001", "SEED-BATCH-DEF-001", "FAILED",
                "ITEM-FAIL-001", "필수 원천값 누락", LocalDateTime.parse("2026-08-26T09:00:00"),
                LocalDateTime.parse("2026-08-26T09:05:00"));
        when(service.listBatchRetryTargets(eq(0), eq(20), any(BatchRetryTargetSearchCriteria.class)))
                .thenReturn(new BatchRetryTargetSearchResponse(List.of(failedTarget), 0, 20, 1));

        mockMvc.perform(get("/api/admin/batch-retries/targets")
                        .param("page", "0")
                        .param("size", "20")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targets[0].originalExecutionId").value("EXEC-FAILED-001"))
                .andExpect(jsonPath("$.data.targets[0].executionStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.targets[0].failedItemKey").value("ITEM-FAIL-001"));
    }

    @Test
    void createBatchRetryRequiresReasonAndPreservesOriginalExecutionWhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/admin/batch-retries")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .header("X-Request-Id", "REQ-RETRY-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalExecutionId\":\"EXEC-FAILED-001\",\"failedItemKey\":\"ITEM-FAIL-001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("retryReason"))
                .andExpect(jsonPath("$.error.fields[0].message").value("재처리 사유를 입력하세요."));
        verify(service, never()).createBatchRetry(any(), any(), any());
    }

    @Test
    void createBatchRetryLinksOriginalExecutionAndStoresSeparateRetryResult() throws Exception {
        BatchRetryResultRow result = new BatchRetryResultRow("RETRY-EXEC-001", "EXEC-FAILED-001", "ITEM-FAIL-001",
                "원천자료 보완 후 재처리", "REQ-RETRY-002", LocalDateTime.parse("2026-08-26T10:00:00"), 1L);
        when(service.createBatchRetry(any(BatchRetryRequest.class), eq(1L), eq("REQ-RETRY-002"))).thenReturn(result);

        mockMvc.perform(post("/api/admin/batch-retries")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .header("X-Request-Id", "REQ-RETRY-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalExecutionId":"EXEC-FAILED-001","failedItemKey":"ITEM-FAIL-001","retryReason":"원천자료 보완 후 재처리"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retryExecutionId").value("RETRY-EXEC-001"))
                .andExpect(jsonPath("$.data.originalExecutionId").value("EXEC-FAILED-001"))
                .andExpect(jsonPath("$.data.retryReason").value("원천자료 보완 후 재처리"))
                .andExpect(jsonPath("$.data.requestId").value("REQ-RETRY-002"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-RETRY-002"));
    }

    @Test
    void createBatchRetryRejectsNonFailedTargetBeforeWritingRetryResult() {
        BatchRetryMapper mapper = mock(BatchRetryMapper.class);
        BatchRetryService retryService = new BatchRetryService(mapper);
        BatchRetryRequest request = new BatchRetryRequest();
        request.setOriginalExecutionId("EXEC-COMPLETED-001");
        request.setRetryReason("완료 대상 재처리 시도");
        when(mapper.countFailedRetryTarget("EXEC-COMPLETED-001", null)).thenReturn(0);

        assertThatThrownBy(() -> retryService.createBatchRetry(request, 1L, "REQ-RETRY-003"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("실패 대상");
        verify(mapper, never()).insertRetryExecution(any(), any(), any(), any(), any(), any(), any());
        verify(mapper, never()).insertRetryResult(any(), any(), any(), any(), any());
    }

    @Test
    void serviceCreatesRetryExecutionAndResultWithoutChangingOriginalResult() {
        BatchRetryMapper mapper = mock(BatchRetryMapper.class);
        BatchRetryService retryService = new BatchRetryService(mapper);
        BatchRetryRequest request = new BatchRetryRequest();
        request.setOriginalExecutionId("EXEC-FAILED-001");
        request.setFailedItemKey("ITEM-FAIL-001");
        request.setRetryReason("원천자료 보완 후 재처리");
        when(mapper.countFailedRetryTarget("EXEC-FAILED-001", "ITEM-FAIL-001")).thenReturn(1);
        when(mapper.findOriginalBatchId("EXEC-FAILED-001")).thenReturn("SEED-BATCH-DEF-001");
        BatchRetryResultRow persisted = new BatchRetryResultRow("RETRY-EXEC-123", "EXEC-FAILED-001", "ITEM-FAIL-001",
                "원천자료 보완 후 재처리", "REQ-RETRY-004", LocalDateTime.parse("2026-08-26T10:30:00"), 1L);
        when(mapper.findRetryResult(any())).thenReturn(persisted);

        BatchRetryResultRow result = retryService.createBatchRetry(request, 1L, "REQ-RETRY-004");

        verify(mapper).insertRetryExecution(any(), eq("SEED-BATCH-DEF-001"), eq("RERUN"),
                eq("원천자료 보완 후 재처리"), eq(1L), eq("EXEC-FAILED-001"), eq("REQ-RETRY-004"));
        verify(mapper).insertRetryResult(any(), eq("EXEC-FAILED-001"), eq("ITEM-FAIL-001"),
                eq("원천자료 보완 후 재처리"), eq("REQ-RETRY-004"));
        verify(mapper, never()).updateOriginalExecutionResult(any(), any());
        assertThat(result.originalExecutionId()).isEqualTo("EXEC-FAILED-001");
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }
}
