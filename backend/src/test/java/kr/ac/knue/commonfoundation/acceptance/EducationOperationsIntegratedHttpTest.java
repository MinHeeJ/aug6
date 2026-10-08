package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import kr.ac.knue.commonfoundation.auth.*;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.courseoperations.*;
import kr.ac.knue.commonfoundation.employmentrateachievements.*;
import kr.ac.knue.commonfoundation.employmentrateimprovements.*;
import kr.ac.knue.commonfoundation.lectureimprovements.*;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** All four real controllers, services and shared guards behind the actual session/menu filter.
 * Persistence adapters are mocked: these tests do not claim PostgreSQL durability or browser coverage.
 */
@WebMvcTest({EmploymentRateImprovementController.class, CourseOperationController.class,
        LectureImprovementController.class, EmploymentRateAchievementController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({EmploymentRateImprovementService.class, EmploymentRateImprovementExceptionAdvice.class,
        CourseOperationService.class, CourseOperationExceptionAdvice.class,
        LectureImprovementService.class, LectureImprovementExceptionAdvice.class,
        EmploymentRateAchievementService.class, EmploymentRateApiAdvice.class,
        EmploymentRateXlsxCodec.class, EducationAchievementGuardService.class})
class EducationOperationsIntegratedHttpTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper json;
    @MockBean EmploymentRateImprovementMapper improvements;
    @MockBean CourseOperationMapper courses;
    @MockBean LectureImprovementMapper lectures;
    @MockBean EmploymentRateAchievementMapper rates;
    @MockBean EducationAchievementGuardMapper guards;
    @MockBean EmploymentRateExcelService excel;
    @MockBean AuthService auth;
    @MockBean EffectivePermissionService permissions;
    private MockMvc mvc;
    private static final LocalDate DATE = LocalDate.parse("2026-04-10");
    private static final String RATE = "/api/business/employment-rate-achievements";

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new AuthenticationFilter(auth, permissions, json)).build();
        when(auth.currentUser(anyString())).thenAnswer(call -> actor(call.getArgument(0)));
        when(permissions.canAccess(anyLong(), anyList(), anyString())).thenReturn(true);
        when(improvements.list(any(), anyLong(), anyList())).thenReturn(List.of());
        when(courses.list(any(), anyLong(), anyList())).thenReturn(List.of());
        when(lectures.list(any(), anyLong(), anyList())).thenReturn(List.of());
        when(rates.list(anyMap())).thenReturn(List.of());
        when(rates.find(anyMap())).thenReturn(null);
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guards.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
    }

    static Stream<Arguments> resources() {
        return Stream.of(
                Arguments.of("employment-rate-improvements", "employment-rate-improvement-achievements"),
                Arguments.of("course-operations", "course-offering-operation-achievements"),
                Arguments.of("lecture-improvements", "teaching-improvement-achievements"),
                Arguments.of("employment-rate-achievements", "employment-rate-achievements"));
    }

    static Stream<Arguments> pages() {
        return resources().flatMap(resource -> Stream.of(20, 50, 100)
                .map(size -> Arguments.of(resource.get()[0], size)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void everyApprovedOperationIsRegisteredInTheMergedMvcApplication() throws Exception {
        Map<String, Object> contract;
        var fixture = new org.springframework.core.io.ClassPathResource("contracts/openapi.yaml");
        try (var stream = fixture.getInputStream()) {
            contract = new org.yaml.snakeyaml.Yaml().load(stream);
        }
        Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) contract.get("paths");
        var mapping = context.getBean(
                "requestMappingHandlerMapping",
                org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.class);
        var registered = new java.util.HashSet<String>();
        mapping.getHandlerMethods().keySet().forEach(info -> info.getPatternValues().forEach(path ->
                info.getMethodsCondition().getMethods().forEach(method -> registered.add(method.name() + " " + path))));
        int checked = 0;
        for (var entry : paths.entrySet()) {
            if (!entry.getKey().matches("/api/business/(employment-rate-improvements|course-operations"
                    + "|lecture-improvements|employment-rate-achievements)(/.*)?")) {
                continue;
            }
            for (String method : entry.getValue().keySet()) {
                if (List.of("get", "post", "put", "delete").contains(method)) {
                    assertThat(registered).contains(method.toUpperCase(java.util.Locale.ROOT) + " " + entry.getKey());
                    checked++;
                }
            }
        }
        assertThat(checked).isGreaterThanOrEqualTo(20);
    }

    @ParameterizedTest
    @MethodSource("resources")
    void canonicalMenuAdmitsFacultyToRealService(String resource, String route) throws Exception {
        mvc.perform(session(get(path(resource)), "R01")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements").isEmpty())
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("integration-trace"));
        verify(permissions).canAccess(101L, List.of("R01"), "/faculty/" + route);
    }

    @ParameterizedTest
    @MethodSource("pages")
    void supportedPageSizesReachApplicationLayer(String resource, int size) throws Exception {
        mvc.perform(session(get(path(resource)).param("page", "1").param("pageSize", String.valueOf(size)), "R01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.pageSize").value(size))
                .andExpect(jsonPath("$.data.page").value(1));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void invalidPageSizeUsesFeatureObjectFields(String resource, String route) throws Exception {
        mvc.perform(session(get(path(resource)).param("pageSize", "10"), "R01"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields.pageSize").exists())
                .andExpect(jsonPath("$.meta.requestId").value("integration-trace"));
        verifyNoInteractions(improvements, courses, lectures, rates);
    }

    @ParameterizedTest
    @MethodSource("resources")
    void missingSessionStopsBeforePersistence(String resource, String route) throws Exception {
        mvc.perform(get(path(resource))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verifyNoInteractions(auth, permissions, improvements, courses, lectures, rates);
    }

    @ParameterizedTest
    @MethodSource("resources")
    void menuDenyStopsBeforeBusinessGuard(String resource, String route) throws Exception {
        when(permissions.canAccess(101L, List.of("R01"), "/faculty/" + route)).thenReturn(false);
        mvc.perform(session(get(path(resource)), "R01")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(improvements, courses, lectures, rates, guards);
    }

    @ParameterizedTest
    @MethodSource("resources")
    void admittedMenuDoesNotGrantR07IndividualReads(String resource, String route) throws Exception {
        mvc.perform(session(get(path(resource)), "R07")).andExpect(status().isForbidden());
        mvc.perform(session(get(path(resource) + "/42"), "R07")).andExpect(status().isForbidden());
        verifyNoInteractions(improvements, courses, lectures, rates, guards);
    }

    @ParameterizedTest
    @MethodSource("resources")
    void missingRequiredItemUsesScopedAdviceWithoutMutation(String resource, String route) throws Exception {
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(body(resource));
        body.remove("managementItemCode");
        mvc.perform(session(post(path(resource)).content(json.writeValueAsString(body)), "R01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.managementItemCode").isString())
                .andExpect(jsonPath("$.meta.requestId").value("integration-trace"));
        verifyNoInteractions(improvements, courses, lectures, rates, guards);
    }

    @ParameterizedTest
    @MethodSource("resources")
    void missingDetailReturns404ThroughFilterAndOwnAdvice(String resource, String route) throws Exception {
        mvc.perform(session(get(path(resource) + "/999"), "R01"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.meta.requestId").value("integration-trace"));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void inactiveInputPeriodStopsCreateBeforeAnyBusinessWrite(String resource, String route) throws Exception {
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        when(rates.organization(101L)).thenReturn("KNUE-DEPT-COMP");
        mvc.perform(session(post(path(resource)).content(body(resource)), "R01"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        noWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = {"EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED"})
    void allFourLockedHeadersRetainDataAndDoNotAppendHistory(String state) throws Exception {
        lockedRows(state, 101L);
        for (String resource : List.of("employment-rate-improvements", "course-operations",
                "lecture-improvements", "employment-rate-achievements")) {
            mvc.perform(session(put(path(resource) + "/42").content(body(resource)), "R01"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code")
                            .value(state.equals("EVALUATION_CONFIRMED")
                                    ? "CONFIRMED_DATA_LOCKED" : "INVALID_STATE_TRANSITION"));
        }
        noWrites();
    }

    @Test
    void foreignOwnerCannotUpdateAnyResourceEvenWithMenuAccess() throws Exception {
        lockedRows("DRAFT", 102L);
        for (String resource : List.of("employment-rate-improvements", "course-operations",
                "lecture-improvements", "employment-rate-achievements")) {
            mvc.perform(session(put(path(resource) + "/42").content(body(resource)), "R01"))
                    .andExpect(status().isForbidden());
        }
        noWrites();
        verifyNoInteractions(guards);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GENERATE", "DELETE"})
    void unapprovedBulkPolicyReturns409WithoutCreatingAnything(String action) throws Exception {
        mvc.perform(session(post(RATE + "/bulk-jobs").content(
                        "{\"evaluationYear\":\"2026\",\"actionType\":\"" + action + "\"}"), "R07"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("BULK_POLICY_NOT_APPROVED"));
        verifyNoInteractions(rates, guards, excel);
        verify(permissions).canAccess(107L, List.of("R07"), "/faculty/employment-rate-achievements");
    }

    @Test
    void r07CannotReadAnotherOperatorsRetainedJob() throws Exception {
        when(rates.job("selected-job")).thenReturn(Map.of("requesterUserId", 102L));
        mvc.perform(session(get(RATE + "/bulk-jobs/selected-job"), "R07"))
                .andExpect(status().isForbidden());
        verify(rates, never()).jobItems(anyString());
    }

    @Test
    void retainedJobReadsActualAdapterResultRatherThanExecutionFixture() throws Exception {
        when(rates.job("selected-job")).thenReturn(Map.of(
                "requesterUserId", 107L, "processedCount", 2, "unprocessedCount", 1));
        when(rates.jobItems("selected-job")).thenReturn(List.of(Map.of(
                "targetUserId", 101L, "processedYn", "N", "unprocessedReason", "정책 미승인")));
        mvc.perform(session(get(RATE + "/bulk-jobs/selected-job"), "R07"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.processedCount").value(2))
                .andExpect(jsonPath("$.data.items[0].unprocessedReason").value("정책 미승인"));
        verify(rates).jobItems("selected-job");
    }

    @Test
    void r07ExportCrossesFilterAndServiceAndReturnsReopenableXlsx() throws Exception {
        when(rates.list(anyMap())).thenReturn(List.of(Map.of(
                "employeeNo", "E0101", "managementItemCode", "EMPLOYMENT_RATE_ACHIEVEMENT",
                "achievementDate", DATE, "achievementName", "=SUM(1,2)")));
        byte[] bytes = mvc.perform(session(get(RATE + "/download"), "R07"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "integration-trace"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new EmploymentRateXlsxCodec().read(bytes).get(1).get(3)).isEqualTo("=SUM(1,2)");
        verify(rates).list(argThat(p -> p.get("userId").equals(107L) && p.get("roles").equals(List.of("R07"))));
    }

    private void lockedRows(String state, Long owner) {
        when(improvements.find(eq(42L), anyBoolean())).thenReturn(new EmploymentRateImprovementRow(
                42L, owner, "KNUE-DEPT-COMP", "2026", "EMPLOYMENT_RATE_IMPROVEMENT", DATE, state,
                DATE, DATE.plusDays(7), "원문", null));
        when(courses.find(eq(42L), anyBoolean())).thenReturn(new CourseOperationRow(
                42L, "EDU-42", owner, "교원", "KNUE-DEPT-COMP", "2026", "COURSE_OPERATION", DATE,
                "원문", state, List.of(), LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00")));
        when(lectures.lock(42L)).thenReturn(new LectureImprovementRow(
                42L, "EDU-42", owner, "KNUE-DEPT-COMP", "2026", "LECTURE_IMPROVEMENT", DATE, state,
                "2026", "1", "원문", List.of()));
        when(rates.lock(anyMap())).thenReturn(Map.of(
                "achievementId", 42L, "teacherUserId", owner, "evaluationYear", "2026", "achievementStatus", state));
    }

    private void noWrites() {
        verify(improvements, never()).insertHeader(anyMap());
        verify(improvements, never()).updateHeader(anyMap());
        verify(improvements, never()).changeHistory(any(), any(), any(), any(), any(), any(), any());
        verify(courses, never()).insertHeader(anyMap());
        verify(courses, never()).updateHeader(anyMap());
        verify(courses, never()).insertHistory(any(), any(), any(), any(), any(), any());
        verify(lectures, never()).insertHeader(anyMap());
        verify(lectures, never()).updateHeader(anyMap());
        verify(lectures, never()).history(anyMap());
        verify(rates, never()).insert(anyMap());
        verify(rates, never()).update(anyMap());
        verify(rates, never()).audit(anyMap());
    }

    private MockHttpServletRequestBuilder session(MockHttpServletRequestBuilder request, String role) {
        return request.cookie(new Cookie(AuthController.SESSION_COOKIE, role))
                .header("X-Request-Id", "integration-trace").contentType(MediaType.APPLICATION_JSON);
    }

    private CurrentUser actor(String role) {
        return new CurrentUser(role.equals("R07") ? 107L : 101L, "faculty", "E0101", "교원",
                List.of(role), List.of());
    }

    private static String path(String resource) {
        return "/api/business/" + resource;
    }

    private String body(String resource) {
        String extra = switch (resource) {
            case "course-operations" -> ",\"performanceDetails\":\"실적내역\"";
            case "lecture-improvements" -> ",\"academicYear\":2026,\"semester\":1,\"achievementContent\":\"개선\"";
            case "employment-rate-achievements" -> ",\"achievementName\":\"취업률 실적\"";
            default -> "";
        };
        return "{\"managementItemCode\":\"" + resource + "\",\"achievementDate\":\"2026-04-10\"" + extra + "}";
    }
}
