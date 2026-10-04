package kr.ac.knue.commonfoundation.acceptance;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
 * Cross-feature acceptance regression coverage for BASIC-83's protected list,
 * mutation, and R07 confirmation boundaries. The test targets the controllers
 * that own the documented routes rather than a shared health or bootstrap slice.
 */
@WebMvcTest(controllers = {
        EmploymentRateImprovementController.class,
        CourseOperationController.class,
        LectureImprovementController.class,
        EmploymentRateAchievementController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic83RegressionAcceptanceTest {
    private static final CurrentUser R01 = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());
    private static final CurrentUser R07 = new CurrentUser(
            107L,
            "excel-operator",
            "E0107",
            "엑셀담당자",
            List.of("R07"),
            List.of());

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

    @Test
    void approvedOpenApiFixtureKeepsBasic83PageSizeSecurityAndConfirmationContracts() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("operationId: listEmploymentRateImprovements")
                .contains("operationId: listCourseOperations")
                .contains("operationId: listLectureImprovements")
                .contains("operationId: listEmploymentRateAchievements")
                .contains("enum: [20, 50, 100]")
                .contains("operationId: createEmploymentRateBulkJob")
                .contains("대상 미리보기와 사용자 확인을 요구한다")
                .contains("OQ-83-01 확정 전 실행조건과 삭제 허용 상태를 hardcode하지 않는다");
    }

    @ParameterizedTest
    @MethodSource("listEndpoints")
    void permittedListPageSizesPreserveTheCallerRequestIdentifier(Endpoint endpoint) throws Exception {
        for (int pageSize : List.of(20, 50, 100)) {
            String requestId = "REQ-B83-PAGE-" + endpoint.name() + "-" + pageSize;

            mockMvc.perform(get(endpoint.path())
                            .requestAttr("currentUser", R01)
                            .param("page", "0")
                            .param("pageSize", String.valueOf(pageSize))
                            .header("X-Request-Id", requestId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.meta.requestId").value(requestId));
        }
    }

    @ParameterizedTest
    @MethodSource("listEndpoints")
    void unsupportedListPageSizeIsRejectedWithoutCallingBusinessServices(Endpoint endpoint) throws Exception {
        mockMvc.perform(get(endpoint.path())
                        .requestAttr("currentUser", R01)
                        .param("pageSize", "25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'pageSize')]").isNotEmpty())
                .andExpect(content().string(not(containsString("java.lang"))));

        verifyNoInteractions(
                employmentRateImprovementService,
                courseOperationService,
                lectureImprovementService,
                employmentRateAchievementService);
    }

    @ParameterizedTest
    @MethodSource("listEndpoints")
    void protectedListsRejectUnauthenticatedRequestsWithTheStandardEnvelope(Endpoint endpoint) throws Exception {
        mockMvc.perform(get(endpoint.path()).param("pageSize", "20"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @ParameterizedTest
    @MethodSource("individualWriteEndpoints")
    void r07CannotBypassR01OnlyIndividualAchievementWrites(Endpoint endpoint) throws Exception {
        mockMvc.perform(post(endpoint.path())
                        .requestAttr("currentUser", R07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endpoint.requestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(content().string(not(containsString("Exception"))));

        verifyNoInteractions(
                employmentRateImprovementService,
                courseOperationService,
                lectureImprovementService,
                employmentRateAchievementService);
    }

    @Test
    void r07ConfirmationRequestSurfacesTheApprovedPolicyConflictInsteadOfCreatingAJob() throws Exception {
        when(employmentRateAchievementService.requestBatchJob(any(), eq(R07)))
                .thenThrow(new ConflictException("OQ-83-01: 실행 정책이 승인되지 않았습니다."));

        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .requestAttr("currentUser", R07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evaluationYear":"2026","actionType":"GENERATE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(content().string(not(containsString("java.lang"))));

        verify(employmentRateAchievementService).requestBatchJob(any(), eq(R07));
    }

    private static Stream<Endpoint> listEndpoints() {
        return Stream.of(
                new Endpoint("employment-rate-improvements", "/api/business/employment-rate-improvements", null),
                new Endpoint("course-operations", "/api/business/course-operations", null),
                new Endpoint("lecture-improvements", "/api/business/lecture-improvements", null),
                new Endpoint("employment-rate-achievements", "/api/business/employment-rate-achievements", null));
    }

    private static Stream<Endpoint> individualWriteEndpoints() {
        return Stream.of(
                new Endpoint(
                        "employment-rate-improvements",
                        "/api/business/employment-rate-improvements",
                        """
                                {"managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT","achievementDate":"2026-04-10"}
                                """),
                new Endpoint(
                        "course-operations",
                        "/api/business/course-operations",
                        """
                                {
                                  "managementItemCode":"COURSE_OPERATION",
                                  "achievementDate":"2026-04-10",
                                  "performanceDetails":"교과목 운영"
                                }
                                """),
                new Endpoint(
                        "lecture-improvements",
                        "/api/business/lecture-improvements",
                        """
                                {
                                  "managementItemCode":"LECTURE_IMPROVEMENT",
                                  "achievementDate":"2026-04-10",
                                  "achievementContent":"개선 내용",
                                  "academicYear":2026,
                                  "semester":"1"
                                }
                                """),
                new Endpoint(
                        "employment-rate-achievements",
                        "/api/business/employment-rate-achievements",
                        """
                                {
                                  "managementItemCode":"EMPLOYMENT_RATE",
                                  "achievementDate":"2026-04-10",
                                  "achievementName":"취업률 실적"
                                }
                                """));
    }

    private record Endpoint(String name, String path, String requestBody) {
    }
}
