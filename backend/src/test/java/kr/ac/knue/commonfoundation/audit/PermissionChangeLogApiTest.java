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

@WebMvcTest(PermissionChangeLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PermissionChangeLogApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean PermissionChangeLogService service;

    @Test
    void listPermissionChangeLogsReturnsEnvelopeWithApproverChangerBeforeAfterAndReason() throws Exception {
        when(service.listPermissionChangeLogs(eq(0), eq(20), any(PermissionChangeLogSearchCriteria.class)))
                .thenReturn(new PermissionChangeLogSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/audit/permission-change-logs")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .param("targetType", "FUNCTION")
                        .param("targetId", "SCR-FUNCTION-PERMISSION-MGMT:R09:UPDATE")
                        .param("approverUserId", "1")
                        .param("changedBy", "1")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-31")
                        .param("page", "0")
                        .param("size", "20")
                        .header("X-Request-Id", "REQ-PERMISSION-CHANGE-QUERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.logs[0].permissionHistoryId").value(91))
                .andExpect(jsonPath("$.data.logs[0].targetType").value("FUNCTION"))
                .andExpect(jsonPath("$.data.logs[0].targetId").value("SCR-FUNCTION-PERMISSION-MGMT:R09:UPDATE"))
                .andExpect(jsonPath("$.data.logs[0].beforeValue").value("{\"permissionAllowed\":\"ALLOW\"}"))
                .andExpect(jsonPath("$.data.logs[0].afterValue").value("{\"permissionAllowed\":\"DENY\"}"))
                .andExpect(jsonPath("$.data.logs[0].approverUserId").value(1))
                .andExpect(jsonPath("$.data.logs[0].approverName").value("시스템 관리자"))
                .andExpect(jsonPath("$.data.logs[0].changedBy").value(1))
                .andExpect(jsonPath("$.data.logs[0].changerName").value("시스템 관리자"))
                .andExpect(jsonPath("$.data.logs[0].reason").value("수정 기능 차단"))
                .andExpect(jsonPath("$.data.logs[0].changedAt").value("2026-08-27T11:10:00"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-PERMISSION-CHANGE-QUERY"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listPermissionChangeLogsRejectsNonR09BeforeMapperLookup() throws Exception {
        mockMvc.perform(get("/api/admin/audit/permission-change-logs")
                        .requestAttr("currentUser", new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of()))
                        .cookie(adminCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).listPermissionChangeLogs(anyInt(), anyInt(), any());
    }

    @Test
    void serviceRejectsInvalidTargetTypeAndDateRangeWithoutMapperLookup() {
        PermissionChangeLogMapper mapper = mock(PermissionChangeLogMapper.class);
        PermissionChangeLogService logService = new PermissionChangeLogService(mapper);
        PermissionChangeLogSearchCriteria invalid = new PermissionChangeLogSearchCriteria(
                null, "UNKNOWN", null, null, LocalDate.parse("2026-08-31"), LocalDate.parse("2026-08-01"));

        assertThatThrownBy(() -> logService.listPermissionChangeLogs(0, 20, invalid))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("권한변경");
        verify(mapper, never()).listPermissionChangeLogs(any(), anyInt(), anyInt());
    }

    @Test
    void serviceDefaultsPermissionChangeLogPaginationToTwentyFiftyOneHundredOnly() {
        PermissionChangeLogMapper mapper = mock(PermissionChangeLogMapper.class);
        PermissionChangeLogService logService = new PermissionChangeLogService(mapper);
        when(mapper.listPermissionChangeLogs(any(), eq(20), eq(0))).thenReturn(List.of());
        when(mapper.countPermissionChangeLogs(any())).thenReturn(0L);

        PermissionChangeLogSearchResponse response = logService.listPermissionChangeLogs(-1, 7,
                new PermissionChangeLogSearchCriteria(null, null, null, null, null, null));

        org.assertj.core.api.Assertions.assertThat(response.page()).isZero();
        org.assertj.core.api.Assertions.assertThat(response.size()).isEqualTo(20);
        verify(mapper).listPermissionChangeLogs(any(), eq(20), eq(0));
    }

    private PermissionChangeLogRow row() {
        return new PermissionChangeLogRow(91L, "FUNCTION", "SCR-FUNCTION-PERMISSION-MGMT:R09:UPDATE",
                "{\"permissionAllowed\":\"ALLOW\"}", "{\"permissionAllowed\":\"DENY\"}", 1L,
                "admin", "시스템 관리자", 1L, "admin", "시스템 관리자", "수정 기능 차단",
                LocalDateTime.parse("2026-08-27T11:10:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }
}
