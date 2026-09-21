package kr.ac.knue.commonfoundation.basic68;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.courseareagroupgrades.CourseAreaGroupGradeItem;
import kr.ac.knue.commonfoundation.courseareagroupgrades.CourseAreaGroupGradeSearchResponse;
import kr.ac.knue.commonfoundation.courseareagroupgrades.CourseAreaGroupGradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = CourseAreaGroupGradeControllerFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseAreaGroupGradeQueryApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean CourseAreaGroupGradeService service;

    /**
     * RED contract for T004: removing the course-area query endpoint or failing to apply all
     * submitted filters must break this observable HTTP response.
     */
    @Test
    void listCourseAreaGroupGradesReturnsOnlyTheMatchingB68SeedResultForR04() throws Exception {
        CurrentUser businessOwner = new CurrentUser(
                1L,
                "admin",
                "ADMIN-001",
                "관리자",
                java.util.List.of("R04"),
                java.util.List.of());
        when(service.list(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new CourseAreaGroupGradeSearchResponse(
                        List.of(new CourseAreaGroupGradeItem(
                                1L,
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

        mockMvc.perform(get("/api/faculty/course-area-group-grades")
                        .requestAttr("currentUser", businessOwner)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "B68-R04-SESSION"))
                        .param("page", "0")
                        .param("pageSize", "20")
                        .param("completionTypeCode", "LIBERAL")
                        .param("semesterCode", "2026-1")
                        .param("courseAreaCode", "LECTURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].completionType").value("LIBERAL"))
                .andExpect(jsonPath("$.data.items[0].semester").value("2026-1"))
                .andExpect(jsonPath("$.data.items[0].courseArea").value("LECTURE"))
                .andExpect(jsonPath("$.data.items[0].groupGrade").value(91.25));
    }

    @Test
    void listAndDownloadCourseAreaGroupGradesAllowTheSystemAdministratorSession() throws Exception {
        CurrentUser systemAdministrator = new CurrentUser(
                1L,
                "admin",
                "E0001",
                "시스템 관리자",
                List.of("R09"),
                List.of());
        when(service.list(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new CourseAreaGroupGradeSearchResponse(List.of(), 0, 20, 0));
        when(service.download(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn("group grades".getBytes());

        mockMvc.perform(get("/api/faculty/course-area-group-grades")
                        .requestAttr("currentUser", systemAdministrator)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "B68-R09-SESSION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(get("/api/faculty/course-area-group-grades/download")
                        .requestAttr("currentUser", systemAdministrator)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "B68-R09-SESSION")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void openApiContractDeclaresTheSystemAdministratorRoleForBothGradeReadOperations() throws Exception {
        String openApi = new String(new ClassPathResource("contracts/openapi.yaml").getInputStream().readAllBytes());
        int listOperation = openApi.indexOf("/api/faculty/course-area-group-grades:\n");
        int downloadOperation = openApi.indexOf("/api/faculty/course-area-group-grades/download:\n");

        org.assertj.core.api.Assertions.assertThat(listOperation).isGreaterThanOrEqualTo(0);
        org.assertj.core.api.Assertions.assertThat(downloadOperation).isGreaterThan(listOperation);
        org.assertj.core.api.Assertions.assertThat(openApi.substring(listOperation, downloadOperation))
                .contains("x-roles: [R01, R04, R09]");
        org.assertj.core.api.Assertions.assertThat(openApi.substring(downloadOperation))
                .contains("x-roles: [R01, R04, R09]");
    }
}

/**
 * Restricts this RED slice to the future BASIC-68 controller without requiring that production
 * type to exist before the contract test is authored.
 */
final class CourseAreaGroupGradeControllerFilter implements TypeFilter {
    private static final String CONTROLLER_TYPE =
            "kr.ac.knue.commonfoundation.courseareagroupgrades.CourseAreaGroupGradeController";

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        return CONTROLLER_TYPE.equals(metadataReader.getClassMetadata().getClassName());
    }
}
