package kr.ac.knue.commonfoundation.securitysessions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
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

@WebMvcTest(ActiveSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ActiveSessionManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ActiveSessionService service;

    @Test
    void listActiveSessionsReturnsEnvelopeWithOnlyActiveSessionStatusFields() throws Exception {
        ActiveSessionRow row = activeSession("SEED-SESSION-ACTIVE-001", "ACTIVE");
        when(service.listActiveSessions(eq(0), eq(20), any(ActiveSessionSearchCriteria.class)))
                .thenReturn(new ActiveSessionSearchResponse(List.of(row), 0, 20, 1));

        mockMvc.perform(get("/api/admin/security/active-sessions")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "20")
                        .header("X-Request-Id", "REQ-ACTIVE-SESSION-QUERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessions[0].sessionId").value("SEED-SESSION-ACTIVE-001"))
                .andExpect(jsonPath("$.data.sessions[0].loginId").value("professor1"))
                .andExpect(jsonPath("$.data.sessions[0].loginAt").value("2026-08-27T09:00:00"))
                .andExpect(jsonPath("$.data.sessions[0].lastAccessedAt").value("2026-08-27T09:20:00"))
                .andExpect(jsonPath("$.data.sessions[0].ipAddress").value("10.0.0.15"))
                .andExpect(jsonPath("$.data.sessions[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-ACTIVE-SESSION-QUERY"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void terminateActiveSessionRequiresReasonAndRecordsRequestIdInResponseMeta() throws Exception {
        ActiveSessionRow terminated = activeSession("SEED-SESSION-ACTIVE-001", "TERMINATED");
        when(service.terminateActiveSession(eq("SEED-SESSION-ACTIVE-001"), any(TerminateActiveSessionRequest.class), eq(1L), eq("REQ-TERM-001")))
                .thenReturn(terminated);

        mockMvc.perform(post("/api/admin/security/active-sessions/SEED-SESSION-ACTIVE-001/terminate")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .header("X-Request-Id", "REQ-TERM-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"비정상 접속 종료\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("SEED-SESSION-ACTIVE-001"))
                .andExpect(jsonPath("$.data.status").value("TERMINATED"))
                .andExpect(jsonPath("$.data.terminatedBy").value(1))
                .andExpect(jsonPath("$.data.terminationReason").value("비정상 접속 종료"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-TERM-001"));
    }

    @Test
    void terminateActiveSessionRejectsNonR09BeforeServiceSideEffect() throws Exception {
        mockMvc.perform(post("/api/admin/security/active-sessions/SEED-SESSION-ACTIVE-001/terminate")
                        .requestAttr("currentUser", new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of()))
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"권한 없음 검증\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).terminateActiveSession(any(), any(), any(), any());
    }

    @Test
    void terminateActiveSessionRequiresReasonBeforeServiceSideEffect() throws Exception {
        mockMvc.perform(post("/api/admin/security/active-sessions/SEED-SESSION-ACTIVE-001/terminate")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("reason"));
        verify(service, never()).terminateActiveSession(any(), any(), any(), any());
    }

    @Test
    void serviceRejectsTerminatingNonActiveSessionWithoutMutation() {
        ActiveSessionMapper mapper = mock(ActiveSessionMapper.class);
        ActiveSessionService sessionService = new ActiveSessionService(mapper);
        TerminateActiveSessionRequest request = new TerminateActiveSessionRequest();
        request.setReason("이미 종료된 세션 검증");
        when(mapper.findSessionForUpdate("SESSION-LOGGED-OUT")).thenReturn(activeSession("SESSION-LOGGED-OUT", "LOGGED_OUT"));

        assertThatThrownBy(() -> sessionService.terminateActiveSession("SESSION-LOGGED-OUT", request, 1L, "REQ-NONACTIVE"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ACTIVE");
        verify(mapper, never()).markTerminated(any(), any(), any(), any());
    }

    @Test
    void listSessionTerminationHistoriesReturnsEnvelopeWithTerminationTypeReasonAndDateFilters() throws Exception {
        SessionTerminationHistoryRow row = sessionHistory("SEED-SESSION-HISTORY-001", "IDLE_TIMEOUT");
        when(service.listSessionTerminationHistories(eq(0), eq(20), any(SessionTerminationHistorySearchCriteria.class)))
                .thenReturn(new SessionTerminationHistorySearchResponse(List.of(row), 0, 20, 1));

        mockMvc.perform(get("/api/admin/security/session-termination-histories")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .param("filter", "홍길동")
                        .param("terminationType", "IDLE_TIMEOUT")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-31")
                        .param("page", "0")
                        .param("size", "20")
                        .header("X-Request-Id", "REQ-SESSION-HISTORY-QUERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.histories[0].sessionId").value("SEED-SESSION-HISTORY-001"))
                .andExpect(jsonPath("$.data.histories[0].loginId").value("professor1"))
                .andExpect(jsonPath("$.data.histories[0].userName").value("홍길동"))
                .andExpect(jsonPath("$.data.histories[0].terminationType").value("IDLE_TIMEOUT"))
                .andExpect(jsonPath("$.data.histories[0].terminatedAt").value("2026-08-27T09:30:00"))
                .andExpect(jsonPath("$.data.histories[0].terminationReason").value("유휴시간 만료"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-SESSION-HISTORY-QUERY"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listSessionTerminationHistoriesRejectsNonR09BeforeServiceLookup() throws Exception {
        mockMvc.perform(get("/api/admin/security/session-termination-histories")
                        .requestAttr("currentUser", new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of()))
                        .cookie(adminCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).listSessionTerminationHistories(anyInt(), anyInt(), any());
    }

    @Test
    void serviceRejectsInvalidSessionTerminationHistoryFilterWithoutMapperLookup() {
        ActiveSessionMapper mapper = mock(ActiveSessionMapper.class);
        ActiveSessionService sessionService = new ActiveSessionService(mapper);
        SessionTerminationHistorySearchCriteria invalid = new SessionTerminationHistorySearchCriteria(
                "홍길동", "BAD_TYPE", LocalDate.parse("2026-08-31"), LocalDate.parse("2026-08-01"));

        assertThatThrownBy(() -> sessionService.listSessionTerminationHistories(0, 20, invalid))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("세션 종료 이력");
        verify(mapper, never()).listSessionTerminationHistories(any(), anyInt(), anyInt());
    }

    @Test
    void serviceDefaultsSessionTerminationHistoryPaginationToAllowedSizes() {
        ActiveSessionMapper mapper = mock(ActiveSessionMapper.class);
        ActiveSessionService sessionService = new ActiveSessionService(mapper);
        SessionTerminationHistorySearchCriteria criteria = new SessionTerminationHistorySearchCriteria(null, null, null, null);
        when(mapper.listSessionTerminationHistories(any(), eq(20), eq(0))).thenReturn(List.of());
        when(mapper.countSessionTerminationHistories(any())).thenReturn(0L);

        SessionTerminationHistorySearchResponse response = sessionService.listSessionTerminationHistories(-1, 7, criteria);

        org.assertj.core.api.Assertions.assertThat(response.page()).isZero();
        org.assertj.core.api.Assertions.assertThat(response.size()).isEqualTo(20);
        verify(mapper).listSessionTerminationHistories(any(), eq(20), eq(0));
    }

    private ActiveSessionRow activeSession(String sessionId, String status) {
        return new ActiveSessionRow(sessionId, 2L, "professor1", "E1001", "홍길동",
                LocalDateTime.parse("2026-08-27T09:00:00"), LocalDateTime.parse("2026-08-27T09:20:00"),
                "10.0.0.15", status, 1L, LocalDateTime.parse("2026-08-27T09:30:00"), "비정상 접속 종료");
    }

    private SessionTerminationHistoryRow sessionHistory(String sessionId, String terminationType) {
        return new SessionTerminationHistoryRow(1L, sessionId, 2L, "professor1", "E1001", "홍길동",
                terminationType, "유휴시간 만료", LocalDateTime.parse("2026-08-27T09:30:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }
}
