package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.*;
import java.util.stream.Stream;
import kr.ac.knue.commonfoundation.auth.*;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.courseoperations.*;
import kr.ac.knue.commonfoundation.employmentrateachievements.*;
import kr.ac.knue.commonfoundation.employmentrateimprovements.*;
import kr.ac.knue.commonfoundation.excel.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import kr.ac.knue.commonfoundation.health.HealthController;
import kr.ac.knue.commonfoundation.lectureimprovements.*;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

/**
 * Exercises merged session/menu/filter/controller/service boundaries, not a mocked business service.
 * Persistence, session lookup and permission adapters are doubles; this is not SQL or browser evidence.
 */
@WebMvcTest({EmploymentRateImprovementController.class, CourseOperationController.class,
        LectureImprovementController.class, EmploymentRateAchievementController.class, HealthController.class})
@AutoConfigureMockMvc
@Import({WebConfig.class, GlobalExceptionHandler.class, EmploymentRateImprovementExceptionHandler.class,
        CourseOperationExceptionHandler.class, LectureImprovementExceptionHandler.class,
        EmploymentRateErrorAdvice.class,
        EmploymentRateImprovementService.class, CourseOperationService.class, LectureImprovementService.class,
        EmploymentRateAchievementService.class, EducationAchievementStatusTransitionPolicy.class,
        EducationAchievementHttpAcceptanceTest.Transactions.class})
class EducationAchievementHttpAcceptanceTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired RequestMappingHandlerMapping mappings;
    @MockBean AuthService auth;
    @MockBean EffectivePermissionService menus;
    @MockBean FunctionPermissionService functions;
    @MockBean EducationAchievementGuardMapper guards;
    @MockBean EmploymentRateImprovementMapper improvements;
    @MockBean CourseOperationMapper courses;
    @MockBean LectureImprovementMapper lectures;
    @MockBean EmploymentRateAchievementMapper employment;
    @MockBean FileStoragePort files;

    private static final List<String> RESOURCES = List.of("employment-rate-improvements", "course-operations",
            "lecture-improvements", "employment-rate-achievements");
    private static final String ROOT = "/api/business/employment-rate-achievements";
    private static final List<String> COLUMNS = List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조");

    // Same DB-free synchronization lifecycle setup as the existing employment API test.
    @TestConfiguration
    static class Transactions {
        @Bean
        PlatformTransactionManager manager() {
            return new AbstractPlatformTransactionManager() {
                protected Object doGetTransaction() { return new Object(); }
                protected void doBegin(Object transaction, TransactionDefinition definition) { }
                protected void doCommit(DefaultTransactionStatus status) { }
                protected void doRollback(DefaultTransactionStatus status) { }
            };
        }
    }

    @BeforeEach
    void allowSeedCompatibleBoundaries() {
        when(menus.canAccess(anyLong(), anyList(), anyString())).thenReturn(true);
        when(improvements.functionAllowed(anyList(), anyString())).thenReturn(1);
        when(employment.function(anyMap())).thenReturn(1);
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guards.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(employment.organization(anyLong())).thenReturn("KNUE-DEPT-COMP");
        // Mockito's empty Map default is not the mapper's missing-row contract (null).
        when(employment.find(anyMap())).thenReturn(null);
        when(employment.template()).thenReturn("employment-template");
        when(employment.templateColumns(anyString())).thenReturn(COLUMNS);
        when(files.save(any())).thenReturn("opaque-acceptance-token");
    }

    private CurrentUser actor(String role) {
        CurrentUser user = new CurrentUser(101L, "faculty", "E0101", "교원", List.of(role), List.of());
        when(auth.currentUser("acceptance-session")).thenReturn(user);
        return user;
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request, String role) {
        actor(role);
        return request.cookie(new Cookie(AuthController.SESSION_COOKIE, "acceptance-session"))
                .header("X-Request-Id", "acceptance-trace");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> approvedPaths() throws Exception {
        try (var stream = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            Map<String, Object> document = new Yaml().load(stream);
            return (Map<String, Map<String, Object>>) document.get("paths");
        }
    }

    private static boolean owned(String path) {
        return RESOURCES.stream().anyMatch(resource -> path.equals("/api/business/" + resource)
                || path.startsWith("/api/business/" + resource + "/"));
    }

    private static Stream<Arguments> operations() throws Exception {
        List<Arguments> operations = new ArrayList<>();
        approvedPaths().forEach((path, methods) -> {
            if (owned(path)) {
                methods.keySet().stream().filter(method -> Set.of("get", "post", "put").contains(method))
                        .forEach(method -> operations.add(Arguments.of(method.toUpperCase(Locale.ROOT), path)));
            }
        });
        for (String suffix : List.of("template", "histories", "{uploadId}/errors", "{uploadId}/errors/download")) {
            operations.add(Arguments.of("GET", ROOT + "/excel-uploads/" + suffix));
        }
        operations.add(Arguments.of("POST", ROOT + "/excel-uploads/{uploadId}/commit"));
        return operations.stream();
    }

    private static Stream<Arguments> readPages() {
        return RESOURCES.stream().flatMap(resource -> Stream.of("R01", "R02", "R04", "R09")
                .flatMap(role -> Stream.of(20, 50, 100).map(size -> Arguments.of(resource, role, size))));
    }

    private static Stream<String> resources() {
        return RESOURCES.stream();
    }

    private static Stream<Arguments> writes() {
        return RESOURCES.stream().flatMap(resource -> Stream.of("POST", "PUT")
                .map(method -> Arguments.of(resource, method)));
    }

    private static String validBody(String resource) {
        String common = "\"managementItemCode\":\"" + resource.toUpperCase(Locale.ROOT).replace('-', '_')
                + "\",\"achievementDate\":\"2026-04-10\"";
        String details = switch (resource) {
            case "employment-rate-improvements" -> """
                    ,"specialLectureStartDate":"2026-04-10","specialLectureEndDate":"2026-04-17",
                    "mockExamQuestionPeriod":"모의고사 출제기간"
                    """;
            case "course-operations" -> ",\"performanceDetails\":\"개설 강좌 운영\"";
            case "lecture-improvements" -> """
                    ,"performanceContent":"강의개선","academicYear":"2025","semester":"2025-1"
                    """;
            default -> ",\"title\":\"취업률 실적\"";
        };
        return "{" + common + details + "}";
    }

    private static MockHttpServletRequestBuilder operation(String method, String template) {
        String path = template.replace("{achievementId}", "999999")
                .replace("{jobId}", "missing-job").replace("{uploadId}", "missing-upload");
        if (method.equals("POST") && path.endsWith("/excel-uploads")) {
            return multipart(path).file(new MockMultipartFile("file", "workbook.xlsx",
                    EmploymentRateWorkbook.CONTENT_TYPE, EmploymentRateWorkbook.write(List.of(COLUMNS))));
        }
        var request = request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON);
        if (path.endsWith("/bulk-jobs")) {
            return request.content("""
                    {"evaluationYear":"2026","actionType":"GENERATE","targetConditionJson":{}}
                    """);
        }
        if (method.equals("POST") || method.equals("PUT")) {
            String resource = path.substring("/api/business/".length()).split("/")[0];
            request.content(validBody(resource));
        }
        return request;
    }

    @Test
    void parsedApprovedOperationsAreRegisteredOnTheirActualControllers() throws Exception {
        Map<String, Set<String>> registered = new HashMap<>();
        mappings.getHandlerMethods().forEach((mapping, handler) -> mapping.getPatternValues().forEach(path -> {
            if (owned(path)) {
                registered.computeIfAbsent(path, ignored -> new HashSet<>()).addAll(
                        mapping.getMethodsCondition().getMethods().stream().map(Enum::name).toList());
            }
        }));
        int operationCount = 0;
        for (var entry : approvedPaths().entrySet()) {
            if (!owned(entry.getKey())) continue;
            for (var method : entry.getValue().keySet()) {
                if (!Set.of("get", "post", "put").contains(method)) continue;
                assertThat(registered.get(entry.getKey())).as(entry.getKey())
                        .contains(method.toUpperCase(Locale.ROOT));
                operationCount++;
            }
        }
        assertThat(operationCount).isEqualTo(20);
        assertThat(registered.values()).allSatisfy(methods -> assertThat(methods).doesNotContain("DELETE"));
    }

    @ParameterizedTest(name = "anonymous: {0} {1}")
    @MethodSource("operations")
    void anonymousCannotDispatchAnyApprovedOrSupplementalOperation(String method, String path) throws Exception {
        mvc.perform(operation(method, path)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verifyNoInteractions(auth, menus, improvements, courses, lectures, employment, files);
    }

    @ParameterizedTest(name = "menu denial: {0} {1}")
    @MethodSource("operations")
    void menuDenialStopsAllSuffixesBeforeBusinessAdapters(String method, String path) throws Exception {
        when(menus.canAccess(anyLong(), anyList(), anyString())).thenReturn(false);
        mvc.perform(authenticated(operation(method, path), "R01")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        String resource = path.substring("/api/business/".length()).split("/")[0];
        verify(menus).canAccess(101L, List.of("R01"), "/faculty/education/" + resource);
        verifyNoInteractions(improvements, courses, lectures, employment, files);
    }

    @ParameterizedTest(name = "business denial after allowed menu: {0} {1}")
    @MethodSource("operations")
    void unrelatedRoleCannotUseMenuGrantAsBusinessAdmission(String method, String path) throws Exception {
        mvc.perform(authenticated(operation(method, path), "R08")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(improvements, courses, lectures, employment, files);
    }

    @ParameterizedTest(name = "{0} {1} pageSize={2}")
    @MethodSource("readPages")
    void admittedReaderTraversesSessionMenuServiceAndEmptyPage(String resource, String role, int size)
            throws Exception {
        mvc.perform(authenticated(get("/api/business/" + resource).param("pageSize", String.valueOf(size)), role))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pageSize").value(size)).andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.achievements").isEmpty())
                .andExpect(jsonPath("$.meta.requestId").value("acceptance-trace"));
        verify(menus).canAccess(101L, List.of(role), "/faculty/education/" + resource);
    }

    @ParameterizedTest
    @MethodSource("resources")
    void invalidPageSizeIsValidationNotAuthenticationFailure(String resource) throws Exception {
        mvc.perform(authenticated(get("/api/business/" + resource).param("pageSize", "21"), "R01"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'pageSize')]").isNotEmpty());
    }

    @ParameterizedTest
    @MethodSource("resources")
    void absentDetailIsNotFoundAfterAuthentication(String resource) throws Exception {
        mvc.perform(authenticated(get("/api/business/" + resource + "/{achievementId}", 999999L), "R01"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        noWrites();
    }

    @ParameterizedTest
    @MethodSource("writes")
    void missingRequiredManagementItemBlocksEveryCreateAndUpdate(String resource, String method) throws Exception {
        String path = "/api/business/" + resource + (method.equals("PUT") ? "/999999" : "");
        String body = validBody(resource).replaceFirst("\"managementItemCode\":\"[^\"]+\",", "");
        mvc.perform(authenticated(request(HttpMethod.valueOf(method), path), "R01")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        noWrites();
    }

    @ParameterizedTest
    @MethodSource("resources")
    void inactivePeriodRejectsCreateWithTypedConflictAndTrace(String resource) throws Exception {
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(authenticated(post("/api/business/" + resource), "R01")
                        .contentType(MediaType.APPLICATION_JSON).content(validBody(resource)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"))
                .andExpect(jsonPath("$.meta.requestId").value("acceptance-trace"));
        noWrites();
    }

    @ParameterizedTest
    @MethodSource("resources")
    void finalizationRejectsAdministratorCreateWithoutBypassingLifecycle(String resource) throws Exception {
        when(guards.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        mvc.perform(authenticated(post("/api/business/" + resource), "R09")
                        .contentType(MediaType.APPLICATION_JSON).content(validBody(resource)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        noWrites();
    }

    @Test
    void unapprovedGenerateReturns409AndDoesNotCreateAnyBusinessOrJobData() throws Exception {
        mvc.perform(authenticated(post(ROOT + "/bulk-jobs"), "R07").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evaluationYear":"2026","actionType":"GENERATE","targetConditionJson":{}}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("BULK_POLICY_NOT_APPROVED"));
        noWrites();
        verify(employment).function(argThat(values -> "EXECUTE".equals(values.get("functionType"))));
        verifyNoMoreInteractions(employment);
    }

    @Test
    void unapprovedDeleteAlsoReturns409WithoutLogicalDeletion() throws Exception {
        mvc.perform(authenticated(post(ROOT + "/bulk-jobs"), "R09").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evaluationYear":"2026","actionType":"DELETE","targetConditionJson":{}}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("BULK_POLICY_NOT_APPROVED"));
        verifyNoInteractions(employment);
    }

    @Test
    void operatorTemplateAndListDownloadAreRealWorkbooks() throws Exception {
        byte[] template = mvc.perform(authenticated(get(ROOT + "/excel-uploads/template"), "R07"))
                .andExpect(status().isOk()).andExpect(content().contentType(EmploymentRateWorkbook.CONTENT_TYPE))
                .andExpect(header().exists("Content-Disposition")).andReturn().getResponse().getContentAsByteArray();
        assertThat(EmploymentRateWorkbook.read(template)).containsExactly(COLUMNS);
        byte[] download = mvc.perform(authenticated(get(ROOT + "/download"), "R07"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(EmploymentRateWorkbook.read(download)).containsExactly(COLUMNS);
        noWrites();
    }

    @Test
    void renamedCsvIsRejectedWithDiagnosticsAndNoDomainMaterialization() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "renamed.xlsx",
                EmploymentRateWorkbook.CONTENT_TYPE,
                "교번,관리항목코드,업적발생일,실적명".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(authenticated(multipart(ROOT + "/excel-uploads").file(file), "R07"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("EXCEL_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.meta.uploadId").isString()).andExpect(jsonPath("$.meta.savedCount").value(0))
                .andExpect(jsonPath("$.meta.errorCount").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("acceptance-trace"));
        verify(employment).upload(anyMap());
        verify(employment).error(anyMap());
        verify(employment).uploadHistory(anyMap());
        verify(employment, never()).insert(anyMap());
    }

    @Test
    void healthRemainsPublicButDoesNotProveComposeReadiness() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));
        verifyNoInteractions(auth, menus);
    }

    private void noWrites() {
        verify(improvements, never()).insertHeader(anyMap());
        verify(improvements, never()).updateHeader(anyLong(), any(), anyString(), anyLong());
        verify(courses, never()).insertHeader(anyMap());
        verify(courses, never()).updateHeader(anyLong(), any(), anyString(), any(), anyLong());
        verify(lectures, never()).insertHeader(anyMap());
        verify(lectures, never()).updateHeader(anyMap());
        verify(employment, never()).insert(anyMap());
        verify(employment, never()).update(anyMap());
        verify(employment, never()).history(anyMap());
        verify(employment, never()).statusHistory(anyMap());
    }
}
