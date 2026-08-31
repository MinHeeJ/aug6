package kr.ac.knue.commonfoundation.basic32;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DataChangeHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DataChangeHistoryApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean DataChangeHistoryService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listDataChangeHistoriesReturnsDefaultTwentyAndFilterableBeforeAfterValuesForReq780Req781Req782() throws Exception {
        DataChangeHistorySearchCriteria criteria = new DataChangeHistorySearchCriteria(
                0,
                20,
                "rejection_reasons",
                "FACULTY_ACHIEVEMENT:DEPT_REVIEW_REQUIRED",
                1L,
                "2026-08-31T00:00:00",
                "2026-08-31T23:59:59",
                "UPDATE");
        when(service.list(criteria, admin)).thenReturn(new DataChangeHistorySearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/data-change-histories")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("targetBusiness", "rejection_reasons")
                        .param("targetKey", "FACULTY_ACHIEVEMENT:DEPT_REVIEW_REQUIRED")
                        .param("changedBy", "1")
                        .param("changedAtFrom", "2026-08-31T00:00:00")
                        .param("changedAtTo", "2026-08-31T23:59:59")
                        .param("changeType", "UPDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.histories[0].targetBusiness").value("rejection_reasons"))
                .andExpect(jsonPath("$.data.histories[0].targetKey").value("FACULTY_ACHIEVEMENT:DEPT_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.data.histories[0].fieldName").value("standard_message"))
                .andExpect(jsonPath("$.data.histories[0].beforeValue").value("기존 문구"))
                .andExpect(jsonPath("$.data.histories[0].afterValue").value("학과장 검토 의견이 필요합니다."))
                .andExpect(jsonPath("$.data.histories[0].changedByName").value("시스템관리자"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void directR01DataChangeHistorySearchIsForbiddenForReq782() throws Exception {
        mockMvc.perform(get("/api/admin/data-change-histories")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any(), any());
    }

    @Test
    void dataChangeHistoryApiIsReadOnlyForReq778Req779() throws Exception {
        mockMvc.perform(post("/api/admin/data-change-histories")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/admin/data-change-histories/1")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie()))
                .andExpect(status().isMethodNotAllowed());
    }

    private DataChangeHistoryRow row() {
        return new DataChangeHistoryRow(
                100L,
                "rejection_reasons",
                "FACULTY_ACHIEVEMENT:DEPT_REVIEW_REQUIRED",
                "UPDATE",
                "standard_message",
                "기존 문구",
                "학과장 검토 의견이 필요합니다.",
                1L,
                "admin",
                "시스템관리자",
                LocalDateTime.parse("2026-08-31T09:00:00"),
                "반려사유 정비");
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
