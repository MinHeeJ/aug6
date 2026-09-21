package kr.ac.knue.commonfoundation.basic68;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.courseareagroupgrades.CourseAreaGroupGradeController;
import kr.ac.knue.commonfoundation.courseareagroupgrades.CourseAreaGroupGradeItem;
import kr.ac.knue.commonfoundation.courseareagroupgrades.CourseAreaGroupGradeSearchResponse;
import kr.ac.knue.commonfoundation.courseareagroupgrades.CourseAreaGroupGradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourseAreaGroupGradeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseAreaGroupGradeRequestIdentifierApiTest {
    @Autowired MockMvc mockMvc;

    @MockBean CourseAreaGroupGradeService service;

    /**
     * RED contract for T014: removing request-identifier propagation from either read endpoint
     * must make the caller unable to correlate the list response and its downloaded file.
     */
    @Test
    void listAndDownloadExposeTheSameSuppliedRequestIdentifier() throws Exception {
        String requestId = "REQ-B68-TRACE-001";
        CurrentUser gradeAdministrator = new CurrentUser(
                4L,
                "faculty-support",
                "E0004",
                "교수지원과 담당자",
                List.of("R04"),
                List.of());
        when(service.list(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new CourseAreaGroupGradeSearchResponse(
                        List.of(new CourseAreaGroupGradeItem(
                                4L,
                                2L,
                                "E0002",
                                "교원",
                                "LIBERAL",
                                "2026-1",
                                "LECTURE",
                                new BigDecimal("91.25"),
                                null)),
                        0,
                        20,
                        1));
        when(service.download(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn("이수구분,학기,교과영역,그룹평가 성적\r\nLIBERAL,2026-1,LECTURE,91.25\r\n".getBytes());

        mockMvc.perform(get("/api/faculty/course-area-group-grades")
                        .requestAttr("currentUser", gradeAdministrator)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "B68-R04-SESSION"))
                        .header("X-Request-Id", requestId)
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.requestId").value(requestId));

        mockMvc.perform(get("/api/faculty/course-area-group-grades/download")
                        .requestAttr("currentUser", gradeAdministrator)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "B68-R04-SESSION"))
                        .header("X-Request-Id", requestId)
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("X-Request-Id", requestId));
    }
}
