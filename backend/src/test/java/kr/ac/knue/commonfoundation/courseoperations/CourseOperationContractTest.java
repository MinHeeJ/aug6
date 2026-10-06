package kr.ac.knue.commonfoundation.courseoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateResponse;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP contract tests reach the actual service and shared guard; only persistence/session ports are mocked. */
class CourseOperationContractTest {
    private static final String PATH = "/api/business/course-operations";
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private CourseOperationMapper mapper;
    private EducationAchievementGuardMapper guardMapper;
    private FunctionPermissionService functions;
    private AuthService auth;
    private EffectivePermissionService menus;
    private MockMvc mvc;
    private CurrentUser user;

    @BeforeEach
    void setup() {
        mapper = mock(CourseOperationMapper.class);
        guardMapper = mock(EducationAchievementGuardMapper.class);
        functions = mock(FunctionPermissionService.class);
        auth = mock(AuthService.class);
        menus = mock(EffectivePermissionService.class);
        user = principal(101L, List.of("R01"));
        when(auth.currentUser("course-test-session")).thenAnswer(call -> user);
        // Final canonical path mapping is owned by T016, not this focused service/HTTP test.
        when(menus.canAccess(anyLong(), anyList(), anyString())).thenReturn(true);
        when(functions.evaluate(any())).thenReturn(new FunctionPermissionEvaluateResponse(
                true, "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT", "R01", "READ", "ALLOW"));
        when(mapper.lockOwner(101L)).thenReturn(101L);
        when(mapper.lockInputPeriods(101L, "2025")).thenReturn(List.of(1L));
        when(guardMapper.countActiveInputPeriods("2025", 101L)).thenReturn(1);
        when(guardMapper.countEvaluationDatePeriods(eq("2025"), eq(101L), any())).thenReturn(1);
        when(mapper.managementRules("FR-030", "2025"))
                .thenReturn(List.of(new CourseOperationManagementRule("Y", "Y", "TEXT")));
        when(mapper.organization(101L)).thenReturn("KNUE-DEPT-COMP");
        CourseOperationService service = new CourseOperationService(
                mapper, new EducationAchievementGuardService(guardMapper), functions, json);
        mvc = MockMvcBuilders.standaloneSetup(new CourseOperationController(service))
                .setControllerAdvice(new CourseOperationExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
                .addFilters(new AuthenticationFilter(auth, menus, json))
                .build();
    }

    @Test
    void createUsesGeneratedHeaderKeyThenReturnsConsistentListAndDetailAndRecordsHistory() throws Exception {
        CourseOperationRow row = row(101L, "DRAFT", "저장한 실적내역");
        doAnswer(call -> {
            Map<String, Object> command = call.getArgument(0);
            assertThat(command.get("userId")).isEqualTo(101L);
            assertThat(command.get("detailJson")).isEqualTo("{\"performanceDetails\":\"저장한 실적내역\"}");
            command.put("achievementId", 81L);
            return 1;
        }).when(mapper).insertHeader(any());
        when(mapper.insertDetail(81L, "저장한 실적내역")).thenReturn(1);
        when(mapper.insertInitialStatus(81L, 101L)).thenReturn(1);
        when(mapper.snapshot(81L)).thenReturn("{\"performance_detail\":\"저장한 실적내역\"}");
        when(mapper.insertHistory(eq(81L), eq("CREATE"), isNull(), anyString(), eq(101L), anyString()))
                .thenReturn(1);
        when(mapper.find(81L)).thenReturn(row);
        when(mapper.canRead(81L, 101L, user.roles())).thenReturn(1);
        when(mapper.list(any(), eq(101L), eq(user.roles()))).thenReturn(List.of(row));
        when(mapper.count(any(), eq(101L), eq(user.roles()))).thenReturn(1L);
        mvc.perform(session(post(PATH)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("2025-04-10", "저장한 실적내역")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.performanceDetails").value("저장한 실적내역"))
                .andExpect(jsonPath("$.meta.requestId").value("course-request"));
        mvc.perform(session(get(PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].performanceDetails").value("저장한 실적내역"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(session(get(PATH + "/81")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceDetails").value("저장한 실적내역"));
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(mapper);
        order.verify(mapper).insertHeader(any());
        order.verify(mapper).insertDetail(81L, "저장한 실적내역");
        order.verify(mapper).insertInitialStatus(81L, 101L);
        order.verify(mapper).snapshot(81L);
        order.verify(mapper).insertHistory(eq(81L), eq("CREATE"), isNull(), anyString(), eq(101L), anyString());
    }

    @Test
    void pathUpdateKeepsStoredYearAndAuditsOldAndNewValuesAndWarnsAboutDate() throws Exception {
        when(mapper.lock(81L)).thenReturn(row(101L, "DRAFT", "이전"));
        when(guardMapper.countEvaluationDatePeriods(eq("2025"), eq(101L), any())).thenReturn(0);
        when(mapper.updateHeader(eq(81L), eq("FR-030"), any(), anyString(), eq("[]"), eq(101L)))
                .thenReturn(1);
        when(mapper.updateDetail(81L, "새 내역")).thenReturn(1);
        when(mapper.snapshot(81L)).thenReturn("{\"performance_detail\":\"이전\"}",
                "{\"performance_detail\":\"새 내역\"}");
        when(mapper.insertHistory(eq(81L), eq("UPDATE"), anyString(), anyString(), eq(101L), anyString()))
                .thenReturn(1);
        CourseOperationRow updated = new CourseOperationRow(
                81L, "CO-test", 101L, "교원", "KNUE-DEPT-COMP", "2025", "FR-030",
                LocalDate.parse("2026-01-01"), "새 내역", "DRAFT", List.of(),
                LocalDateTime.parse("2025-04-10T09:00:00"), LocalDateTime.parse("2025-04-11T09:00:00"));
        when(mapper.find(81L)).thenReturn(updated);
        when(mapper.canRead(81L, 101L, user.roles())).thenReturn(1);
        mvc.perform(session(put(PATH + "/81")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-01-01", "새 내역")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2025"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        mvc.perform(session(get(PATH + "/81")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceDetails").value("새 내역"));
        verify(guardMapper).countActiveInputPeriods("2025", 101L);
        verify(mapper).insertHistory(81L, "UPDATE", "{\"performance_detail\":\"이전\"}",
                "{\"performance_detail\":\"새 내역\"}", 101L, "course-request");
    }

    @Test
    void missingPerformanceDetailsOnBothCommandsIsFieldLevel400AndWritesNothing() throws Exception {
        for (MockHttpServletRequestBuilder request : List.of(post(PATH), put(PATH + "/81"))) {
            mvc.perform(session(request).contentType(MediaType.APPLICATION_JSON).content("""
                    {"managementItemCode":"FR-030","achievementDate":"2025-04-10"}
                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[?(@.field == 'performanceDetails')]").isNotEmpty());
        }
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateDetail(any(), any());
    }

    @Test
    void ownerAndStatusInjectionOrMalformedDateAre400WithoutSensitiveLeakage() throws Exception {
        for (String extra : List.of("\"achievementStatus\":\"EVALUATION_CONFIRMED\"", "\"teacherUserId\":102")) {
            mvc.perform(session(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(
                            body("2025-04-10", "입력").replace("}", "," + extra + "}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.message").value("입력값을 확인해 주세요."));
        }
        mvc.perform(session(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(body("not-a-date", "입력")))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void unauthorizedRolesCannotWriteEvenIfMenuAllowsAccess() throws Exception {
        for (String role : List.of("R02", "R04", "R07", "R09")) {
            user = principal(101L, List.of(role));
            mvc.perform(session(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(body("2025-04-10", "입력")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void anonymousIs401AndMenuDenialStopsTheService() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        when(menus.canAccess(anyLong(), anyList(), anyString())).thenReturn(false);
        mvc.perform(session(get(PATH))).andExpect(status().isForbidden());
        verify(mapper, never()).list(any(), any(), any());
    }

    @Test
    void otherOwnerUpdateAndOutOfScopeDetailAreForbiddenWithoutMutation() throws Exception {
        when(mapper.lock(81L)).thenReturn(row(102L, "DRAFT", "원본"));
        when(mapper.find(81L)).thenReturn(row(102L, "DRAFT", "원본"));
        mvc.perform(session(put(PATH + "/81")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("2025-04-10", "새값")))
                .andExpect(status().isForbidden());
        mvc.perform(session(get(PATH + "/81"))).andExpect(status().isForbidden());
        verify(mapper, never()).updateDetail(any(), any());
    }

    @Test
    void confirmedAndSubmittedRowsRejectUpdatesAndRemainUnchanged() throws Exception {
        for (String state : List.of("EVALUATION_CONFIRMED", "SUBMITTED", "DEPARTMENT_CONFIRMED", "CERTIFIED")) {
            when(mapper.lock(81L)).thenReturn(row(101L, state, "원본"));
            mvc.perform(session(put(PATH + "/81")).contentType(MediaType.APPLICATION_JSON)
                            .content(body("2025-04-10", "변경값")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("EVALUATION_CONFIRMED".equals(state)
                            ? "CONFIRMED_DATA_LOCKED" : "INVALID_STATE_TRANSITION"));
        }
        verify(mapper, never()).updateDetail(any(), any());
        verify(mapper, never()).insertHistory(any(), any(), any(), any(), any(), any());
    }

    @Test
    void finalizationAndInactiveInputPeriodBlockWritesWithPeriodTakingPriority() throws Exception {
        when(mapper.lock(81L)).thenReturn(row(101L, "DRAFT", "원본"));
        when(guardMapper.countEvaluationConfirmations(101L, "2025")).thenReturn(1);
        mvc.perform(session(put(PATH + "/81")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("2025-04-10", "변경값")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        when(mapper.lockInputPeriods(101L, "2025")).thenReturn(List.of());
        mvc.perform(session(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(body("2025-04-10", "변경값")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        verify(mapper, never()).updateDetail(any(), any());
    }

    @Test
    void nonexistentPathUpdateIs404AndCannotCreate() throws Exception {
        mvc.perform(session(put(PATH + "/999")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("2025-04-10", "내역")))
                .andExpect(status().isNotFound());
        mvc.perform(session(get(PATH + "/999"))).andExpect(status().isNotFound());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void filtersPaginationAndMultiRoleScopesAreIdenticalForListAndCount() throws Exception {
        user = principal(101L, List.of("R01", "R02", "R04"));
        when(mapper.list(any(), eq(101L), eq(user.roles()))).thenReturn(List.of(row(102L, "DRAFT", "다른 교원")));
        when(mapper.count(any(), eq(101L), eq(user.roles()))).thenReturn(1L);
        mvc.perform(session(get(PATH)).param("page", "2").param("pageSize", "50")
                        .param("managementItemCode", " FR-030 ").param("teacherName", " 교원 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.page").value(2));
        ArgumentCaptor<CourseOperationSearchCriteria> listed =
                ArgumentCaptor.forClass(CourseOperationSearchCriteria.class);
        ArgumentCaptor<CourseOperationSearchCriteria> counted =
                ArgumentCaptor.forClass(CourseOperationSearchCriteria.class);
        verify(mapper).list(listed.capture(), eq(101L), eq(user.roles()));
        verify(mapper).count(counted.capture(), eq(101L), eq(user.roles()));
        assertThat(listed.getValue()).isEqualTo(counted.getValue());
        assertThat(listed.getValue().offset()).isEqualTo(100);
        assertThat(listed.getValue().managementItemCode()).isEqualTo("FR-030");
        mvc.perform(session(get(PATH)).param("pageSize", "10")).andExpect(status().isBadRequest());
    }

    @Test
    void ambiguousReadonlyOrWrongTypeManagementItemsRejectWithoutWrites() throws Exception {
        for (List<CourseOperationManagementRule> rules : List.of(
                List.of(new CourseOperationManagementRule("N", "Y", "TEXT")),
                List.of(new CourseOperationManagementRule("Y", "Y", "NUMBER")),
                List.of(new CourseOperationManagementRule("Y", "Y", "TEXT"),
                        new CourseOperationManagementRule("Y", "Y", "TEXT")))) {
            when(mapper.managementRules("FR-030", "2025")).thenReturn(rules);
            mvc.perform(session(post(PATH)).contentType(MediaType.APPLICATION_JSON)
                            .content(body("2025-04-10", "문자값")))
                    .andExpect(status().isBadRequest());
        }
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void missingFunctionPermissionAndNewAttachmentReferencesFailClosed() throws Exception {
        when(functions.evaluate(any())).thenThrow(new ForbiddenException());
        mvc.perform(session(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(body("2025-04-10", "값")))
                .andExpect(status().isForbidden());
        // Override the throwing stub without invoking it during restubbing.
        doReturn(new FunctionPermissionEvaluateResponse(true, "screen", "R01", "CREATE", "ALLOW"))
                .when(functions).evaluate(any());
        mvc.perform(session(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(
                        body("2025-04-10", "값").replace("[]", "[\"unverified-token\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'attachmentIds')]").isNotEmpty());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void approvedFixtureIsLoadedOnlyFromBackendClasspath() throws Exception {
        try (var input = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThat(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .contains("operationId: listCourseOperations", "operationId: createCourseOperation",
                            "operationId: getCourseOperation", "operationId: updateCourseOperation");
        }
    }

    private MockHttpServletRequestBuilder session(MockHttpServletRequestBuilder request) {
        return request.cookie(new Cookie(AuthController.SESSION_COOKIE, "course-test-session"))
                .header("X-Request-Id", "course-request");
    }

    private String body(String date, String details) throws Exception {
        return json.writeValueAsString(Map.of("managementItemCode", "FR-030", "achievementDate", date,
                "performanceDetails", details, "attachmentIds", List.of()));
    }

    private CurrentUser principal(Long id, List<String> roles) {
        return new CurrentUser(id, "teacher-" + id, "E" + id, "교원", roles, List.of());
    }

    private CourseOperationRow row(Long owner, String state, String details) {
        return new CourseOperationRow(81L, "CO-test", owner, "교원", "KNUE-DEPT-COMP", "2025", "FR-030",
                LocalDate.parse("2025-04-10"), details, state, List.of(),
                LocalDateTime.parse("2025-04-10T09:00:00"), LocalDateTime.parse("2025-04-10T09:00:00"));
    }
}
