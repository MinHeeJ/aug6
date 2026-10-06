package kr.ac.knue.commonfoundation.taskgaps;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateAchievementController;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateAchievementMapper;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateAchievementService;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateInputErrorHandler;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateWorkbook;
import kr.ac.knue.commonfoundation.excel.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateResponse;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * T012 HTTP coverage through the actual filter, controller and service, with no database context.
 * Session lookup, permission storage and SQL adapters are isolated mocks, not runtime DB proof.
 * Pending policy deliberately has no successful execution fixture: no 202 or job is fabricated.
 */
class EmploymentRateBulkJobContractTest {
    private static final String BASE = "/api/business/employment-rate-achievements/bulk-jobs";
    private static final Long ACTOR = 701L;
    private final EmploymentRateAchievementMapper mapper = mock(EmploymentRateAchievementMapper.class);
    private final FunctionPermissionService functions = mock(FunctionPermissionService.class);
    private final AuthService sessions = mock(AuthService.class);
    private final EffectivePermissionService menus = mock(EffectivePermissionService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        EmploymentRateAchievementService service = new EmploymentRateAchievementService(
                mapper,
                mock(EducationAchievementGuardService.class),
                functions,
                mock(FileStoragePort.class),
                json,
                mock(EmploymentRateWorkbook.class));
        mvc = MockMvcBuilders.standaloneSetup(new EmploymentRateAchievementController(service))
                .setControllerAdvice(new EmploymentRateInputErrorHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
                .addFilters(new AuthenticationFilter(sessions, menus, json))
                .build();
        // Only the permission backing store is mocked; the servlet filter remains active.
        when(menus.canAccess(anyLong(), anyList(), anyString())).thenReturn(true);
        when(functions.evaluate(any())).thenAnswer(invocation -> {
            var request = (kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest)
                    invocation.getArgument(0);
            return new FunctionPermissionEvaluateResponse(
                    true, request.screenId(), request.roleCode(), request.functionType(), "ALLOW");
        });
        login("R07");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GENERATE", "DELETE"})
    void pendingPolicyReturns409WithoutAnyPersistenceCall(String action) throws Exception {
        mvc.perform(command(BASE, """
                {"evaluationYear":"2025","actionType":"%s","targetCondition":{}}
                """.formatted(action)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message", containsString("POLICY_NOT_APPROVED")))
                .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"R01", "R02", "R04", "R09"})
    void menuAccessDoesNotGrantBulkExecution(String role) throws Exception {
        login(role);
        mvc.perform(command(BASE, """
                {"evaluationYear":"2025","actionType":"GENERATE"}
                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(functions, mapper);
    }

    @Test
    void missingSessionStopsAtAuthenticationFilter() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verifyNoInteractions(sessions, functions, mapper);
    }

    @Test
    void menuDenialStopsBeforeService() throws Exception {
        when(menus.canAccess(anyLong(), anyList(), anyString())).thenReturn(false);
        mvc.perform(command(BASE, """
                {"evaluationYear":"2025","actionType":"GENERATE"}
                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(functions, mapper);
    }

    @Test
    void executeFunctionDenialStopsBeforePolicyAndPersistence() throws Exception {
        doThrow(new ForbiddenException()).when(functions).evaluate(any());
        mvc.perform(command(BASE, """
                {"evaluationYear":"2025","actionType":"GENERATE"}
                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(mapper);
    }

    @Test
    void missingEvaluationYearReportsFieldValidation() throws Exception {
        mvc.perform(command(BASE, "{\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("evaluationYear"));
        verifyNoInteractions(functions, mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"targetCondition", "targetConditionJson"})
    void bothContractSpellingsValidateRatherThanIgnoreUnsupportedConditions(String field) throws Exception {
        mvc.perform(command(BASE, """
                {"evaluationYear":"2025","actionType":"GENERATE","%s":{"teacherUserId":99}}
                """.formatted(field)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("targetCondition"));
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"targetCondition", "targetConditionJson"})
    void previewUsesActualScopedCountAndDoesNotAuthorizeExecution(String field) throws Exception {
        Map<String, Object> query = Map.of("evaluationYear", "2025", "managementItemCode", "ITEM-A");
        when(mapper.count(eq(query), eq(ACTOR), eq(List.of("R07")))).thenReturn(2L);
        mvc.perform(command(BASE + "/preview", """
                {"evaluationYear":"2025","actionType":"DELETE","%s":{"managementItemCode":"ITEM-A"}}
                """.formatted(field)).header("X-Request-Id", "bulk-preview-trace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.existingAchievementCount").value(2))
                .andExpect(jsonPath("$.data.policyApproved").value(false))
                .andExpect(jsonPath("$.meta.requestId").value("bulk-preview-trace"));
        verify(mapper).count(query, ACTOR, List.of("R07"));
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void resultIncludesCountsAndUnprocessedReasonsForOwner() throws Exception {
        when(mapper.job("stored-job", ACTOR)).thenReturn(Map.of(
                "jobId", "stored-job", "totalCount", 2, "processedCount", 1,
                "unprocessedCount", 1, "targetCondition", "{}"));
        when(mapper.jobItems("stored-job", ACTOR)).thenReturn(List.of(
                Map.of("targetUserId", 801L, "processedYn", "Y"),
                Map.of("targetUserId", 802L, "processedYn", "N", "unprocessedReason", "대상 제외")));
        mvc.perform(get(BASE + "/stored-job").cookie(cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.processedCount").value(1))
                .andExpect(jsonPath("$.data.unprocessedCount").value(1))
                .andExpect(jsonPath("$.data.items[1].processedYn").value("N"))
                .andExpect(jsonPath("$.data.items[1].unprocessedReason").value("대상 제외"));
        verify(mapper).job("stored-job", ACTOR);
        verify(mapper).jobItems("stored-job", ACTOR);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void unknownOrOtherOwnersJobReturns404WithoutFetchingItems() throws Exception {
        // MyBatis returns null for no matching owner row; Mockito otherwise defaults to an empty Map.
        when(mapper.job("invisible-job", ACTOR)).thenReturn(null);
        mvc.perform(get(BASE + "/invisible-job").cookie(cookie()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        verify(mapper).job("invisible-job", ACTOR);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "17", "true", "\"\"", "\"   \""})
    void invalidFilterValuesNeverReachPersistence(String value) throws Exception {
        for (String field : List.of("targetCondition", "targetConditionJson")) {
            for (String suffix : List.of("", "/preview")) {
                mvc.perform(command(BASE + suffix, """
                        {"evaluationYear":"2025","actionType":"DELETE","%s":{"managementItemCode":%s}}
                        """.formatted(field, value)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                        .andExpect(jsonPath("$.error.fields[0].field").value("targetCondition"));
            }
        }
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "17", "true", "\"condition\""})
    void conditionMustBeAnObject(String value) throws Exception {
        mvc.perform(command(BASE + "/preview", """
                {"evaluationYear":"2025","actionType":"GENERATE","targetCondition":%s}
                """.formatted(value)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("targetCondition"));
        verifyNoInteractions(mapper);
    }

    @Test
    void previewNormalizesTextAndOmitsNullFiltersWithoutExpandingRoleScope() throws Exception {
        when(sessions.currentUser("bulk-contract-session")).thenReturn(new CurrentUser(
                ACTOR, "bulk-contract", "EMP-701", "담당자", List.of("R01", "R07", "R09"), List.of()));
        Map<String, Object> query = Map.of("evaluationYear", "2025", "managementItemCode", "ITEM-A");
        when(mapper.count(query, ACTOR, List.of("R07"))).thenReturn(0L);
        mvc.perform(command(BASE + "/preview", """
                {"evaluationYear":"2025","actionType":"GENERATE","targetConditionJson":{
                    "managementItemCode":" ITEM-A ","managementNo":null,"certificationStatus":null}}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.existingAchievementCount").value(0))
                .andExpect(jsonPath("$.data.policyApproved").value(false));
        verify(mapper).count(query, ACTOR, List.of("R07"));
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"R01", "R02", "R04", "R09"})
    void nonBulkRolesCannotPreviewOrReadJobResults(String role) throws Exception {
        login(role);
        mvc.perform(command(BASE + "/preview", """
                {"evaluationYear":"2025","actionType":"DELETE"}
                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mvc.perform(get(BASE + "/stored-job").cookie(cookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(functions, mapper);
    }

    @Test
    void resultRequiresReadPermissionRatherThanExecutePermission() throws Exception {
        doThrow(new ForbiddenException()).when(functions).evaluate(any());
        mvc.perform(get(BASE + "/stored-job").cookie(cookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(functions).evaluate(new FunctionPermissionEvaluateRequest(
                "SCR-EMPLOYMENT-RATE-ACHIEVEMENT", "R07", "READ", "DRAFT", null));
        verifyNoInteractions(mapper);
    }

    @Test
    void storedConditionIsDecodedAndResultCarriesRequestId() throws Exception {
        when(mapper.job("stored-job", ACTOR)).thenReturn(Map.of(
                "jobId", "stored-job", "totalCount", 0, "processedCount", 0,
                "unprocessedCount", 0, "targetCondition", "{\"managementItemCode\":\"ITEM-A\"}"));
        when(mapper.jobItems("stored-job", ACTOR)).thenReturn(List.of());
        mvc.perform(get(BASE + "/stored-job").cookie(cookie()).header("X-Request-Id", "result-trace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value("stored-job"))
                .andExpect(jsonPath("$.data.targetCondition.managementItemCode").value("ITEM-A"))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.meta.requestId").value("result-trace"));
        verify(mapper).job("stored-job", ACTOR);
        verify(mapper).jobItems("stored-job", ACTOR);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"targetCondition", "targetConditionJson"})
    void explicitNullConditionMatchesOmittedCondition(String field) throws Exception {
        Map<String, Object> query = Map.of("evaluationYear", "2025");
        when(mapper.count(query, ACTOR, List.of("R07"))).thenReturn(3L);
        mvc.perform(command(BASE + "/preview", """
                {"evaluationYear":"2025","actionType":"GENERATE","%s":null}
                """.formatted(field)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.existingAchievementCount").value(3))
                .andExpect(jsonPath("$.data.policyApproved").value(false));
        mvc.perform(command(BASE, """
                {"evaluationYear":"2025","actionType":"GENERATE","%s":null}
                """.formatted(field)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message", containsString("POLICY_NOT_APPROVED")));
        verify(mapper).count(query, ACTOR, List.of("R07"));
        verifyNoMoreInteractions(mapper);
    }

    private void login(String role) {
        when(sessions.currentUser("bulk-contract-session")).thenReturn(
                new CurrentUser(ACTOR, "bulk-contract", "EMP-701", "담당자", List.of(role), List.of()));
    }

    private Cookie cookie() {
        return new Cookie("COMMON_FOUNDATION_SESSION", "bulk-contract-session");
    }

    private MockHttpServletRequestBuilder command(String path, String body) {
        return post(path).cookie(cookie()).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
