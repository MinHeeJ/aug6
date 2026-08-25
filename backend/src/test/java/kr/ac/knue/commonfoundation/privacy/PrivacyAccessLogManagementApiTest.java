package kr.ac.knue.commonfoundation.privacy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@WebMvcTest(PrivacyAccessLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PrivacyAccessLogManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean PrivacyAccessLogService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    private final CurrentUser reader = new CurrentUser(2L, "reader", "E0002", "조회 사용자", List.of("R01"), List.of());

    @Test
    void searchPrivacyAccessLogsFiltersByActorTargetProcessTypeAndPeriodForReq207() throws Exception {
        PrivacyAccessLogSearchCriteria criteria = new PrivacyAccessLogSearchCriteria(0, 20, 1L, "TARGET-2026-001", "VIEW", "2026-08-01", "2026-08-31");
        when(service.searchPrivacyAccessLogs(criteria)).thenReturn(new PrivacyAccessLogSearchResponse(List.of(logRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/privacy/access-logs")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .param("actorUserId", "1")
                        .param("targetRef", "TARGET-2026-001")
                        .param("processType", "VIEW")
                        .param("processedFrom", "2026-08-01")
                        .param("processedTo", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.logs[0].processType").value("VIEW"))
                .andExpect(jsonPath("$.data.logs[0].targetRef").value("TARGET-2026-001"))
                .andExpect(jsonPath("$.data.logs[0].processResult").value("SUCCESS"));
    }

    @Test
    void getPrivacyAccessLogDetailIsReadOnlyAndDoesNotExposeOriginalPersonalValueForReq208() throws Exception {
        when(service.getPrivacyAccessLog(9001L)).thenReturn(logRow());

        mockMvc.perform(get("/api/admin/privacy/access-logs/9001")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.historyId").value(9001))
                .andExpect(jsonPath("$.data.processPurpose").value("감사 검증 목적"))
                .andExpect(jsonPath("$.data.actualValue").doesNotExist())
                .andExpect(jsonPath("$.data.originalValue").doesNotExist())
                .andExpect(jsonPath("$.data.plainValue").doesNotExist());
    }

    @Test
    void recordPrivacyAccessLogPersistsSideEffectAndReturnsCreatedHistoryForReq209() throws Exception {
        when(service.recordPrivacyAccessLog(any(), eq("203.0.113.10"))).thenReturn(logRow());

        mockMvc.perform(post("/api/admin/privacy/access-logs-record")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"processType":"VIEW","actorUserId":1,"targetRef":"TARGET-2026-001","processPurpose":"감사 검증 목적","requestIp":"203.0.113.10","processResult":"SUCCESS"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.historyId").value(9001))
                .andExpect(jsonPath("$.data.requestIp").value("203.0.113.10"));
    }

    @Test
    void recordPrivacyAccessLogRejectsMissingPurposeBeforePersistenceForReq209() throws Exception {
        PrivacyAccessLogMapper mapper = mock(PrivacyAccessLogMapper.class);
        PrivacyAccessLogService accessLogService = new PrivacyAccessLogService(mapper);

        assertThatThrownBy(() -> accessLogService.recordPrivacyAccessLog(
                new PrivacyAccessLogRecordRequest("VIEW", 1L, "TARGET-2026-001", "", "127.0.0.1", "SUCCESS", null, null), "127.0.0.1"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("처리 목적");
        verify(mapper, never()).insertPrivacyAccessLog(any(), any(), any(), any(), any(), any());
    }

    @Test
    void privacyAccessLogUpdateAndDeleteOperationsAreAbsentForReq210() throws Exception {
        mockMvc.perform(put("/api/admin/privacy/access-logs/9001")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/admin/privacy/access-logs/9001")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie()))
                .andExpect(status().isMethodNotAllowed());
        verify(service, never()).recordPrivacyAccessLog(any(), any());
    }

    @Test
    void searchPrivacyAccessLogsRejectsNonR09Session() throws Exception {
        mockMvc.perform(get("/api/admin/privacy/access-logs")
                        .requestAttr("currentUser", reader)
                        .cookie(adminCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).searchPrivacyAccessLogs(any());
    }

    private PrivacyAccessLogRow logRow() {
        return new PrivacyAccessLogRow(9001L, "VIEW", 1L, "admin", "TARGET-2026-001", "감사 검증 목적",
                LocalDateTime.parse("2026-08-25T09:10:00"), "203.0.113.10", "SUCCESS");
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
