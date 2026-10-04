package kr.ac.knue.commonfoundation.acceptance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.courseoperations.CourseOperationController;
import kr.ac.knue.commonfoundation.courseoperations.CourseOperationService;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateAchievementController;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateAchievementService;
import kr.ac.knue.commonfoundation.employmentrateimprovements.EmploymentRateImprovementAchievementController;
import kr.ac.knue.commonfoundation.employmentrateimprovements.EmploymentRateImprovementAchievementService;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementAchievementController;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementAchievementService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

/**
 * Regression coverage for BASIC-83's shared request tracing, role boundaries,
 * permitted page sizes, and approved API-operation inventory.
 */
@WebMvcTest({
        EmploymentRateImprovementAchievementController.class,
        CourseOperationController.class,
        LectureImprovementAchievementController.class,
        EmploymentRateAchievementController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic83EducationAchievementsAcceptanceTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateImprovementAchievementService employmentRateImprovementService;

    @MockBean
    private CourseOperationService courseOperationService;

    @MockBean
    private LectureImprovementAchievementService lectureImprovementService;

    @MockBean
    private EmploymentRateAchievementService employmentRateAchievementService;

    private final CurrentUser r01 = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L,
            "excel-operator",
            "E0107",
            "엑셀담당자",
            List.of("R07"),
            List.of());

    @Test
    void approvedFixtureListsEveryBasic83Operation() throws Exception {
        String openApi = StreamUtils.copyToString(
                new ClassPathResource("contracts/openapi.yaml").getInputStream(),
                StandardCharsets.UTF_8);

        Assertions.assertThat(openApi)
                .contains("operationId: listEmploymentRateImprovements")
                .contains("operationId: createEmploymentRateImprovement")
                .contains("operationId: listCourseOperations")
                .contains("operationId: createCourseOperation")
                .contains("operationId: listLectureImprovements")
                .contains("operationId: createLectureImprovement")
                .contains("operationId: listEmploymentRateAchievements")
                .contains("operationId: downloadEmploymentRateAchievements")
                .contains("operationId: uploadEmploymentRateAchievementsExcel")
                .contains("operationId: createEmploymentRateBulkJob")
                .contains("operationId: getEmploymentRateBulkJob");
    }

    @Test
    void allBusinessListsRejectUnauthenticatedRequestsWithoutAnInternalError() throws Exception {
        assertUnauthenticated("/api/business/employment-rate-improvements");
        assertUnauthenticated("/api/business/course-operations");
        assertUnauthenticated("/api/business/lecture-improvements");
        assertUnauthenticated("/api/business/employment-rate-achievements");
    }

    @Test
    void allBusinessListsAcceptApprovedPageSizesAndPreserveRequestIds() throws Exception {
        assertListRequest("/api/business/employment-rate-improvements", 20, "REQ-B83-ERI");
        assertListRequest("/api/business/course-operations", 50, "REQ-B83-COURSE");
        assertListRequest("/api/business/lecture-improvements", 100, "REQ-B83-LECTURE");
        assertListRequest("/api/business/employment-rate-achievements", 20, "REQ-B83-ER");
    }

    @Test
    void r07CannotCreateFacultyAchievementsAndR01CannotStartABulkJob() throws Exception {
        assertForbiddenRegularCreate("/api/business/employment-rate-improvements", """
                {"managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT","achievementDate":"2026-04-10"}
                """);
        assertForbiddenRegularCreate("/api/business/course-operations", """
                {"managementItemCode":"COURSE_OPERATION","achievementDate":"2026-04-10",
                "performanceDetails":"현장실습 강좌 운영"}
                """);
        assertForbiddenRegularCreate("/api/business/lecture-improvements", """
                {"managementItemCode":"LECTURE_IMPROVEMENT","achievementDate":"2026-04-10",
                "achievementContent":"수업 개선","academicYear":2026,"semester":1}
                """);
        assertForbiddenRegularCreate("/api/business/employment-rate-achievements", """
                {"managementItemCode":"EMPLOYMENT_RATE","achievementDate":"2026-04-10"}
                """);

        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evaluationYear":"2026","actionType":"GENERATE","targetCondition":{}}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private void assertUnauthenticated(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    private void assertListRequest(String path, int pageSize, String requestId) throws Exception {
        mockMvc.perform(get(path)
                        .param("page", "0")
                        .param("pageSize", String.valueOf(pageSize))
                        .header("X-Request-Id", requestId)
                        .requestAttr("currentUser", r01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.requestId").value(requestId));
    }

    private void assertForbiddenRegularCreate(String path, String body) throws Exception {
        mockMvc.perform(post(path)
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }
}
