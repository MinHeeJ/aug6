package kr.ac.knue.commonfoundation.acceptance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import kr.ac.knue.commonfoundation.employmentrateimprovements.EmploymentRateImprovementController;
import kr.ac.knue.commonfoundation.employmentrateimprovements.EmploymentRateImprovementService;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementController;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementService;
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
 * Cross-slice acceptance regression checks for the four BASIC-83 education
 * achievement route groups. These checks keep common boundary rules aligned
 * without replacing each slice's persistence-level tests.
 */
@WebMvcTest(controllers = {
        EmploymentRateImprovementController.class,
        CourseOperationController.class,
        LectureImprovementController.class,
        EmploymentRateAchievementController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic83CrossSliceAcceptanceTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateImprovementService employmentRateImprovementService;

    @MockBean
    private CourseOperationService courseOperationService;

    @MockBean
    private LectureImprovementService lectureImprovementService;

    @MockBean
    private EmploymentRateAchievementService employmentRateAchievementService;

    private final CurrentUser r07 = new CurrentUser(
            107L,
            "excel-operator",
            "E0107",
            "엑셀담당자",
            List.of("R07"),
            List.of());

    @Test
    void approvedContractContainsAllBasic83OperationsAndRoleObligations() throws Exception {
        String openApi = StreamUtils.copyToString(
                new ClassPathResource("contracts/openapi.yaml").getInputStream(),
                StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("operationId: listEmploymentRateImprovements")
                .contains("operationId: createEmploymentRateImprovement")
                .contains("operationId: listCourseOperations")
                .contains("operationId: createCourseOperation")
                .contains("operationId: listLectureImprovements")
                .contains("operationId: createLectureImprovement")
                .contains("operationId: listEmploymentRateAchievements")
                .contains("operationId: createEmploymentRateAchievement")
                .contains("operationId: uploadEmploymentRateAchievementsExcel")
                .contains("operationId: createEmploymentRateBulkJob")
                .contains("operationId: getEmploymentRateBulkJob")
                .contains("x-roles:");
    }

    @Test
    void protectedWriteRoutesRejectTheExcelOnlyRoleBeforeCallingServices() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employmentRateImprovementRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseOperationRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lectureImprovementRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employmentRateAchievementRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(employmentRateImprovementService, never()).create(any(), any(), any());
        verify(courseOperationService, never()).create(any(), any(), any());
        verify(lectureImprovementService, never()).create(any(), any(), any());
        verify(employmentRateAchievementService, never()).create(any(), any(), any());
    }

    @Test
    void controllersWithBoundaryPageValidationRejectUnsupportedPageSizes() throws Exception {
        CurrentUser r01 = new CurrentUser(
                101L,
                "faculty",
                "E0101",
                "교원",
                List.of("R01"),
                List.of());

        mockMvc.perform(get("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r01)
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private String employmentRateImprovementRequest() {
        return """
                {
                  "managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT",
                  "achievementDate":"2026-04-10"
                }
                """;
    }

    private String courseOperationRequest() {
        return """
                {
                  "managementItemCode":"COURSE_OPERATION",
                  "achievementDate":"2026-04-10",
                  "performanceDetails":"강좌 개설 운영"
                }
                """;
    }

    private String lectureImprovementRequest() {
        return """
                {
                  "managementItemCode":"LECTURE_IMPROVEMENT",
                  "achievementDate":"2026-04-10",
                  "achievementContent":"강의 개선",
                  "academicYear":2026,
                  "semester":1
                }
                """;
    }

    private String employmentRateAchievementRequest() {
        return """
                {
                  "managementItemCode":"EMPLOYMENT_RATE",
                  "achievementDate":"2026-04-10",
                  "achievementName":"취업률 실적"
                }
                """;
    }
}
