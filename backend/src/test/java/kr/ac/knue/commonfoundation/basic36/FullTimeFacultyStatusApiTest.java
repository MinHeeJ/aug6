package kr.ac.knue.commonfoundation.basic36;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
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

@WebMvcTest(FullTimeFacultyStatusController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FullTimeFacultyStatusApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean FullTimeFacultyStatusService service;

    private final CurrentUser departmentChair = new CurrentUser(3L, "dept-chair", "E2003", "학과장", List.of("R03"), List.of());
    private final CurrentUser businessOwner = new CurrentUser(4L, "business-owner", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E1001", "교원", List.of("R01"), List.of());

    @Test
    void listFullTimeFacultyStatusesFiltersByBaseYearAndOrganizationForReq1251Req1252() throws Exception {
        when(service.list(new FullTimeFacultyStatusSearchCriteria(0, 20, 2026, "KNUE-DEPT-COMP", "", "")))
                .thenReturn(new FullTimeFacultyStatusSearchResponse(List.of(row()), 0, 20, 1, 2026));

        mockMvc.perform(get("/api/admin/full-time-faculty-statuses")
                        .requestAttr("currentUser", departmentChair)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B36-FACULTY-LIST")
                        .param("baseYear", "2026")
                        .param("organizationCode", "KNUE-DEPT-COMP")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.baseYear").value(2026))
                .andExpect(jsonPath("$.data.statuses[0].employeeNo").value("E1001"))
                .andExpect(jsonPath("$.data.statuses[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data.statuses[0].collegeName").value("교육과학대학"))
                .andExpect(jsonPath("$.data.statuses[0].departmentName").value("컴퓨터교육과"))
                .andExpect(jsonPath("$.data.statuses[0].rankName").value("교수"))
                .andExpect(jsonPath("$.data.statuses[0].retirementDate").value("2026-12-31"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B36-FACULTY-LIST"));
    }

    @Test
    void listFullTimeFacultyStatusesRejectsMissingBaseYearForReq1253() throws Exception {
        mockMvc.perform(get("/api/admin/full-time-faculty-statuses")
                        .requestAttr("currentUser", businessOwner)
                        .cookie(sessionCookie())
                        .param("organizationCode", "KNUE-DEPT-COMP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("baseYear"));
        verify(service, never()).list(any());
    }

    @Test
    void r01CannotListFullTimeFacultyStatusesForReq1260() throws Exception {
        mockMvc.perform(get("/api/admin/full-time-faculty-statuses")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .param("baseYear", "2026"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void listFullTimeFacultyStatusesRejectsUnsupportedPageSizeForReq1198() throws Exception {
        mockMvc.perform(get("/api/admin/full-time-faculty-statuses")
                        .requestAttr("currentUser", businessOwner)
                        .cookie(sessionCookie())
                        .param("baseYear", "2026")
                        .param("size", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("size"));
        verify(service, never()).list(any());
    }

    private FullTimeFacultyStatusRow row() {
        return new FullTimeFacultyStatusRow("E1001", "홍길동", "KNUE-COL-EDU", "교육과학대학",
                "KNUE-DEPT-COMP", "컴퓨터교육과", "교수", LocalDate.parse("2026-12-31"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
