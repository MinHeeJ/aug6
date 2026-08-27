package kr.ac.knue.commonfoundation.audit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BusinessProcessLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BusinessProcessLogApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean BusinessProcessLogService service;

    @Test
    void listBusinessProcessLogsReturnsEnvelopeWithActionStatesActorResultAndRequestId() throws Exception {
        BusinessProcessLogRow row = businessLog("SEED-BUSINESS-AUDIT-001", "UPDATE", "SUCCESS");
        when(service.listBusinessProcessLogs(eq(0), eq(20), any(BusinessProcessLogSearchCriteria.class)))
                .thenReturn(new BusinessProcessLogSearchResponse(List.of(row), 0, 20, 1));

        mockMvc.perform(get("/api/admin/audit/business-process-logs")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .param("filter", "admin")
                        .param("actionType", "UPDATE")
                        .param("targetKey", "SEED-BUSINESS-AUDIT-001")
                        .param("actorUserId", "1")
                        .param("resultStatus", "SUCCESS")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-31")
                        .param("page", "0")
                        .param("size", "20")
                        .header("X-Request-Id", "REQ-BUSINESS-AUDIT-QUERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.logs[0].auditLogId").value(1))
                .andExpect(jsonPath("$.data.logs[0].actionType").value("UPDATE"))
                .andExpect(jsonPath("$.data.logs[0].targetKey").value("SEED-BUSINESS-AUDIT-001"))
                .andExpect(jsonPath("$.data.logs[0].beforeState").value("{\"status\": \"BEFORE\"}"))
                .andExpect(jsonPath("$.data.logs[0].afterState").value("{\"status\": \"AFTER\"}"))
                .andExpect(jsonPath("$.data.logs[0].actorUserId").value(1))
                .andExpect(jsonPath("$.data.logs[0].actorLoginId").value("admin"))
                .andExpect(jsonPath("$.data.logs[0].actorName").value("시스템 관리자"))
                .andExpect(jsonPath("$.data.logs[0].resultStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.logs[0].requestId").value("REQ-BUSINESS-AUDIT-SEED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-BUSINESS-AUDIT-QUERY"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listBusinessProcessLogsRejectsNonR09BeforeMapperLookup() throws Exception {
        mockMvc.perform(get("/api/admin/audit/business-process-logs")
                        .requestAttr("currentUser", new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of()))
                        .cookie(adminCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).listBusinessProcessLogs(anyInt(), anyInt(), any());
    }

    @Test
    void serviceRejectsInvalidActionResultAndDateRangeWithoutMapperLookup() {
        BusinessProcessLogMapper mapper = mock(BusinessProcessLogMapper.class);
        BusinessProcessLogService logService = new BusinessProcessLogService(mapper);
        BusinessProcessLogSearchCriteria invalid = new BusinessProcessLogSearchCriteria(
                "admin", "BAD_ACTION", "TARGET", 1L, "BAD_RESULT", LocalDate.parse("2026-08-31"), LocalDate.parse("2026-08-01"));

        assertThatThrownBy(() -> logService.listBusinessProcessLogs(0, 20, invalid))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("업무처리 로그");
        verify(mapper, never()).listBusinessProcessLogs(any(), anyInt(), anyInt());
    }

    @Test
    void serviceDefaultsBusinessProcessLogPaginationToAllowedSizes() {
        BusinessProcessLogMapper mapper = mock(BusinessProcessLogMapper.class);
        BusinessProcessLogService logService = new BusinessProcessLogService(mapper);
        when(mapper.listBusinessProcessLogs(any(), eq(20), eq(0))).thenReturn(List.of());
        when(mapper.countBusinessProcessLogs(any())).thenReturn(0L);

        BusinessProcessLogSearchResponse response = logService.listBusinessProcessLogs(-1, 7,
                new BusinessProcessLogSearchCriteria(null, null, null, null, null, null, null));

        org.assertj.core.api.Assertions.assertThat(response.page()).isZero();
        org.assertj.core.api.Assertions.assertThat(response.size()).isEqualTo(20);
        verify(mapper).listBusinessProcessLogs(any(), eq(20), eq(0));
    }

    private BusinessProcessLogRow businessLog(String targetKey, String actionType, String resultStatus) {
        return new BusinessProcessLogRow(1L, actionType, targetKey, "{\"status\": \"BEFORE\"}", "{\"status\": \"AFTER\"}",
                1L, "admin", "시스템 관리자", resultStatus, "REQ-BUSINESS-AUDIT-SEED", LocalDateTime.parse("2026-08-27T09:30:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }
}
