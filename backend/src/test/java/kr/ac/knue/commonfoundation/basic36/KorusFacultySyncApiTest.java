package kr.ac.knue.commonfoundation.basic36;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KorusFacultySyncController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class KorusFacultySyncApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean KorusFacultySyncService service;

    private final CurrentUser businessOwner = new CurrentUser(4L, "business-owner", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E1001", "교원", List.of("R01"), List.of());

    @Test
    void listKorusFacultySyncResultsReturnsSuccessFailureRowsAndRequestIdForReq1238() throws Exception {
        when(service.list(new KorusFacultySyncSearchCriteria(0, 20, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "FAILED", "REQ-KORUS", "E1001")))
                .thenReturn(new KorusFacultySyncSearchResponse(List.of(resultRow("FAILED")), 0, 20, 1));

        mockMvc.perform(get("/api/admin/korus-faculty-sync-results")
                        .requestAttr("currentUser", businessOwner)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B36-KORUS-LIST")
                        .param("targetStartDate", "2026-01-01")
                        .param("targetEndDate", "2026-01-31")
                        .param("syncStatus", "FAILED")
                        .param("requestId", "REQ-KORUS")
                        .param("employeeNo", "E1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].requestId").value("REQ-KORUS-001"))
                .andExpect(jsonPath("$.data.results[0].employeeNo").value("E1001"))
                .andExpect(jsonPath("$.data.results[0].organizationCode").value("KNUE-DEPT-COMP"))
                .andExpect(jsonPath("$.data.results[0].syncStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.results[0].errorMessage").value("조직 매핑 실패"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B36-KORUS-LIST"));
    }

    @Test
    void r01CannotListKorusFacultySyncResultsForReq1238() throws Exception {
        mockMvc.perform(get("/api/admin/korus-faculty-sync-results")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void listKorusFacultySyncResultsRejectsInvalidPageSizeForReq1238() throws Exception {
        mockMvc.perform(get("/api/admin/korus-faculty-sync-results")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .param("size", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("size"));
        verify(service, never()).list(any());
    }

    @Test
    void createKorusFacultySyncRunRecordsSideEffectsAndReturnsRunForReq1239Req1242() throws Exception {
        when(service.createRun(any(KorusFacultySyncRunRequest.class), eq(1L), eq("REQ-B36-KORUS-RUN"))).thenReturn(runRow());

        mockMvc.perform(post("/api/admin/korus-faculty-sync-runs")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B36-KORUS-RUN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStartDate":"2026-01-01","targetEndDate":"2026-01-31"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value("REQ-B36-KORUS-RUN"))
                .andExpect(jsonPath("$.data.runType").value("MANUAL"))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failureCount").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B36-KORUS-RUN"));
        verify(service).createRun(any(KorusFacultySyncRunRequest.class), eq(1L), eq("REQ-B36-KORUS-RUN"));
    }

    @Test
    void createKorusFacultySyncRunRejectsMissingTargetStartDateForReq1240() throws Exception {
        mockMvc.perform(post("/api/admin/korus-faculty-sync-runs")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetEndDate":"2026-01-31"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(service, never()).createRun(any(), any(), any());
    }

    @Test
    void createKorusFacultySyncRunRejectsDuplicateRequestWithoutMutatingForReq1244() throws Exception {
        when(service.createRun(any(KorusFacultySyncRunRequest.class), eq(1L), eq("REQ-B36-DUP")))
                .thenThrow(new ConflictException("이미 처리된 KORUS 교원 동기화 요청입니다."));

        mockMvc.perform(post("/api/admin/korus-faculty-sync-runs")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B36-DUP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStartDate":"2026-01-01","targetEndDate":"2026-01-31"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void createKorusFacultySyncRetryRejectsSuccessfulResultForReq1245() throws Exception {
        when(service.retryFailedResult(eq(700L), eq(1L), eq("REQ-B36-KORUS-RETRY")))
                .thenThrow(new BusinessValidationException("실패 건만 재처리할 수 있습니다.", List.of(new ValidationError("resultId", "성공 건은 재처리 대상이 아닙니다."))));

        mockMvc.perform(post("/api/admin/korus-faculty-sync-results/700/retry")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B36-KORUS-RETRY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("resultId"));
    }

    @Test
    void createKorusFacultySyncRetryCreatesRetryRunForFailedResultForReq1245() throws Exception {
        when(service.retryFailedResult(eq(700L), eq(1L), eq("REQ-B36-KORUS-RETRY"))).thenReturn(runRow("RETRY"));

        mockMvc.perform(post("/api/admin/korus-faculty-sync-results/700/retry")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B36-KORUS-RETRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runType").value("RETRY"))
                .andExpect(jsonPath("$.data.requestId").value("REQ-B36-KORUS-RETRY"));
    }

    private KorusFacultySyncResultRow resultRow(String status) {
        return new KorusFacultySyncResultRow(700L, 70L, "REQ-KORUS-001", "E1001", "홍길동", "KNUE-DEPT-COMP", "교수", "E1001-APPT", status, "조직 매핑 실패", null, LocalDateTime.parse("2026-01-02T03:04:05"));
    }

    private KorusFacultySyncRunRow runRow() {
        return runRow("MANUAL");
    }

    private KorusFacultySyncRunRow runRow(String runType) {
        return new KorusFacultySyncRunRow(70L, "REQ-B36-KORUS-RUN".replace("RUN", runType.equals("RETRY") ? "RETRY" : "RUN"), runType, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "PARTIAL", 3, 2, 1, 1L, LocalDateTime.parse("2026-01-02T03:00:00"), LocalDateTime.parse("2026-01-02T03:04:05"), null);
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
