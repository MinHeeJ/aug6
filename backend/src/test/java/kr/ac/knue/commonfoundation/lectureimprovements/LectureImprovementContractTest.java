package kr.ac.knue.commonfoundation.lectureimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.AuthService;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Real HTTP controller/service/guard with the session filter active; SQL effects are verified at its adapter. */
class LectureImprovementContractTest {
    private static final String BASE = "/api/business/lecture-improvements";
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private LectureImprovementMapper mapper;
    private EducationAchievementGuardMapper guardMapper;
    private FunctionPermissionService permissions;
    private AuthService auth;
    private EffectivePermissionService menus;
    private MockMvc mvc;
    private CurrentUser faculty;

    @BeforeEach
    void setup() {
        mapper = mock(LectureImprovementMapper.class);
        guardMapper = mock(EducationAchievementGuardMapper.class);
        permissions = mock(FunctionPermissionService.class);
        auth = mock(AuthService.class);
        menus = mock(EffectivePermissionService.class);
        faculty = user(101L, List.of("R01"));
        when(auth.currentUser("faculty-session")).thenReturn(faculty);
        when(menus.canAccess(anyLong(), any(), anyString())).thenReturn(true);
        when(permissions.evaluate(any())).thenAnswer(invocation -> {
            var request = (kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest)
                    invocation.getArgument(0);
            return new FunctionPermissionEvaluateResponse(
                    true, request.screenId(), request.roleCode(), request.functionType(), "ALLOW");
        });
        when(guardMapper.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guardMapper.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.codeOptions("ACADEMIC_YEAR"))
                .thenReturn(List.of(new LectureImprovementOption("2025", "2025학년도")));
        when(mapper.codeOptions("SEMESTER")).thenReturn(List.of(
                new LectureImprovementOption("1", "1학기"), new LectureImprovementOption("2", "2학기")));
        when(mapper.managementItems(any())).thenReturn(List.of(
                new LectureImprovementManagementItem("FR-031", "강의개선", "Y", "TEXT", "Y")));
        when(mapper.organization(101L)).thenReturn("DEPT-TEST");
        var service = new LectureImprovementService(
                mapper, new EducationAchievementGuardService(guardMapper), permissions, json);
        mvc = MockMvcBuilders.standaloneSetup(new LectureImprovementController(service))
                .setControllerAdvice(new GlobalExceptionHandler(), new LectureImprovementExceptionHandler())
                .addFilters(new AuthenticationFilter(auth, menus, json))
                .build();
    }

    @Test
    void classpathContractRemainsAvailable() throws Exception {
        try (var stream = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThat(stream.read()).isNotEqualTo(-1);
        }
    }

    @Test
    void createUsesHeaderGeneratedKeyThenDetailThenHistoriesAndDetailIsReadable() throws Exception {
        when(mapper.insertHeader(any(), eq(101L), eq("2025"), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(82L);
        when(mapper.find(82L, false)).thenReturn(row(101L, "DRAFT", "수업 개선", "2025", 1));
        when(mapper.snapshot(82L)).thenReturn("{\"detail\":{\"academic_year\":2025,\"semester_code\":\"1\"}}");
        when(mapper.canRead(82L, faculty)).thenReturn(1);

        mvc.perform(session(post(BASE)).contentType(MediaType.APPLICATION_JSON).content(body().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("lecture-request"))
                .andExpect(jsonPath("$.data.achievement.achievementId").value(82))
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(1));
        InOrder order = inOrder(mapper);
        order.verify(mapper).insertHeader(any(), eq(101L), eq("2025"), anyString(), anyString(), anyString(), eq("[]"));
        order.verify(mapper).insertDetail(eq(82L), any());
        order.verify(mapper).insertStatusHistory(82L, 101L);
        order.verify(mapper).snapshot(82L);
        order.verify(mapper).insertChangeHistory(eq(82L), isNull(), anyString(), eq(101L), eq("lecture-request"));
        mvc.perform(session(get(BASE + "/82")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementContent").value("수업 개선"))
                .andExpect(jsonPath("$.data.academicYear").value(2025))
                .andExpect(jsonPath("$.data.semester").value(1));
    }

    @Test
    void updateKeepsHeaderEvaluationYearAndFullBeforeAfterSnapshotWhileChangingSemester() throws Exception {
        when(mapper.find(82L, true)).thenReturn(row(101L, "DRAFT", "이전", "2025", 1));
        when(mapper.find(82L, false)).thenReturn(row(101L, "DRAFT", "수정", "2025", 2));
        when(mapper.updateHeader(eq(82L), any(), eq(101L), anyString(), anyString())).thenReturn(1);
        String before = "{\"achievement\":{\"evaluation_year\":\"2025\"},\"detail\":{\"semester_code\":\"1\"}}";
        String after = "{\"achievement\":{\"evaluation_year\":\"2025\"},\"detail\":{\"semester_code\":\"2\"}}";
        when(mapper.snapshot(82L)).thenReturn(before, after);
        when(guardMapper.countEvaluationDatePeriods("2025", 101L, LocalDate.parse("2026-04-10")))
                .thenReturn(0);
        ObjectNode body = body().put("achievementDate", "2026-04-10").put("semester", 2)
                .put("achievementContent", "수정");

        mvc.perform(session(put(BASE + "/82")).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2025"))
                .andExpect(jsonPath("$.data.achievement.semester").value(2))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        verify(guardMapper).countActiveInputPeriods("2025", 101L);
        verify(guardMapper).countEvaluationDatePeriods("2025", 101L, LocalDate.parse("2026-04-10"));
        verify(mapper).insertChangeHistory(82L, before, after, 101L, "lecture-request");
        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
        verify(mapper).updateHeader(eq(82L), any(), eq(101L), detail.capture(), eq("[]"));
        assertThat(json.readTree(detail.getValue()).get("semester").asInt()).isEqualTo(2);
        assertThat(json.readTree(detail.getValue()).get("achievementContent").asText()).isEqualTo("수정");
    }

    @Test
    void filteredListAndTotalUseSamePrincipalAndCriteriaWithMultipleRoles() throws Exception {
        CurrentUser multiRole = user(101L, List.of("R01", "R02", "R04"));
        when(auth.currentUser("faculty-session")).thenReturn(multiRole);
        when(mapper.list(any(), eq(multiRole))).thenReturn(List.of(row(102L, "DRAFT", "조회", "2025", 1)));
        when(mapper.count(any(), eq(multiRole))).thenReturn(1L);
        mvc.perform(session(get(BASE).param("teacherName", "교원").param("pageSize", "50")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements.length()").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(50));
        ArgumentCaptor<LectureImprovementSearchCriteria> list =
                ArgumentCaptor.forClass(LectureImprovementSearchCriteria.class);
        ArgumentCaptor<LectureImprovementSearchCriteria> count =
                ArgumentCaptor.forClass(LectureImprovementSearchCriteria.class);
        verify(mapper).list(list.capture(), eq(multiRole));
        verify(mapper).count(count.capture(), eq(multiRole));
        assertThat(list.getValue()).isEqualTo(count.getValue());
        assertThat(list.getValue().teacherName()).isEqualTo("교원");
    }

    @ParameterizedTest
    @ValueSource(strings = {"R02", "R04", "R07", "R09"})
    void mutationRequiresR01EvenWhenMenuAllowsAccess(String role) throws Exception {
        when(auth.currentUser("faculty-session")).thenReturn(user(101L, List.of(role)));
        mvc.perform(session(post(BASE)).contentType(MediaType.APPLICATION_JSON).content(body().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(mapper, never()).insertHeader(any(), any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"R07", "R09"})
    void readsRejectRolesOutsideApprovedReadContract(String role) throws Exception {
        when(auth.currentUser("faculty-session")).thenReturn(user(101L, List.of(role)));
        mvc.perform(session(get(BASE))).andExpect(status().isForbidden());
        mvc.perform(session(get(BASE + "/82"))).andExpect(status().isForbidden());
        verify(mapper, never()).list(any(), any());
        verify(mapper, never()).find(any(), anyBoolean());
    }

    @Test
    void updateRejectsInvalidSemesterWithoutChangingHeaderOrDetail() throws Exception {
        mvc.perform(session(put(BASE + "/82")).contentType(MediaType.APPLICATION_JSON)
                        .content(body().put("semester", 9).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("semester"));
        verify(mapper, never()).updateHeader(any(), any(), any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any());
    }

    @Test
    void missingSessionIs401AndMenuDenialIs403() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        when(menus.canAccess(anyLong(), any(), anyString())).thenReturn(false);
        mvc.perform(session(get(BASE))).andExpect(status().isForbidden());
        verify(mapper, never()).list(any(), any());
    }

    @Test
    void functionDenialCannotBeReplacedByMenuPermission() throws Exception {
        doThrow(new ForbiddenException()).when(permissions).evaluate(any());
        mvc.perform(session(post(BASE)).contentType(MediaType.APPLICATION_JSON).content(body().toString()))
                .andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(any(), any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"academicYear", "semester", "achievementContent", "achievementDate"})
    void missingRequiredFieldsReturn400(String field) throws Exception {
        ObjectNode body = body();
        body.remove(field);
        mvc.perform(session(post(BASE)).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == '" + field + "')]").isNotEmpty());
    }

    @Test
    void semesterAndYearMustExistInActiveDbChoices() throws Exception {
        mvc.perform(session(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content(body().put("semester", 9).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("semester"));
        mvc.perform(session(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content(body().put("academicYear", 2026).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("academicYear"));
        verify(mapper, never()).insertHeader(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void duplicateOrNonEditableManagementItemIs400() throws Exception {
        var item = new LectureImprovementManagementItem("FR-031", "강의개선", "Y", "TEXT", "Y");
        when(mapper.managementItems("2025")).thenReturn(List.of(item, item));
        mvc.perform(session(post(BASE)).contentType(MediaType.APPLICATION_JSON).content(body().toString()))
                .andExpect(status().isBadRequest());
        when(mapper.managementItems("2025")).thenReturn(List.of(
                new LectureImprovementManagementItem("FR-031", "강의개선", "Y", "TEXT", "N")));
        mvc.perform(session(post(BASE)).contentType(MediaType.APPLICATION_JSON).content(body().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmedAndSubmittedRowsAre409WithNoMutationsAndPeriodHasPriority() throws Exception {
        for (String status : List.of("EVALUATION_CONFIRMED", "SUBMITTED")) {
            LectureImprovementRow original = row(101L, status, "원본", "2025", 1);
            when(mapper.find(82L, true)).thenReturn(original);
            mvc.perform(session(put(BASE + "/82")).contentType(MediaType.APPLICATION_JSON).content(body().toString()))
                    .andExpect(status().isConflict());
            assertThat(original.achievementContent()).isEqualTo("원본");
        }
        when(guardMapper.countActiveInputPeriods("2025", 101L)).thenReturn(0);
        mvc.perform(session(put(BASE + "/82")).contentType(MediaType.APPLICATION_JSON).content(body().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message")
                        .value(org.hamcrest.Matchers.containsString("PERIOD_NOT_ACTIVE")));
        verify(mapper, never()).updateHeader(any(), any(), any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any());
    }

    @Test
    void evaluationFinalizationAlsoLocksDraftRow() throws Exception {
        when(mapper.find(82L, true)).thenReturn(row(101L, "DRAFT", "원본", "2025", 1));
        when(guardMapper.countEvaluationConfirmations(101L, "2025")).thenReturn(1);
        mvc.perform(session(put(BASE + "/82")).contentType(MediaType.APPLICATION_JSON).content(body().toString()))
                .andExpect(status().isConflict());
        verify(mapper, never()).updateHeader(any(), any(), any(), any(), any());
    }

    @Test
    void otherOwnerIs403ForUpdateAndDetailAndNonexistentUpdateIs404() throws Exception {
        when(mapper.find(82L, true)).thenReturn(row(102L, "DRAFT", "타인", "2025", 1));
        when(mapper.find(82L, false)).thenReturn(row(102L, "DRAFT", "타인", "2025", 1));
        mvc.perform(session(put(BASE + "/82")).contentType(MediaType.APPLICATION_JSON).content(body().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(session(get(BASE + "/82"))).andExpect(status().isForbidden());
        mvc.perform(session(put(BASE + "/9999")).contentType(MediaType.APPLICATION_JSON).content(body().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(session(get(BASE + "/9999"))).andExpect(status().isNotFound());
        verify(mapper, never()).updateHeader(any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"achievementId", "teacherUserId", "achievementStatus", "status", "evaluationYear"})
    void serverOwnedFieldInjectionIsRejected(String field) throws Exception {
        mvc.perform(session(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content(body().put(field, "CLIENT_INJECTED").toString()))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void arbitraryAttachmentTokenIsRejectedUntilSharedOwnershipAdapterIsAvailable() throws Exception {
        ObjectNode body = body();
        body.putArray("attachmentIds").add("unknown-token");
        mvc.perform(session(post(BASE)).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("attachmentIds"));
    }

    @Test
    void persistenceFailureDoesNotLeakSensitiveSqlInHttpError() throws Exception {
        when(mapper.list(any(), any())).thenThrow(new IllegalStateException("password=secret SELECT internal_table"));
        String response = mvc.perform(session(get(BASE)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("password", "secret", "internal_table");
    }

    private MockHttpServletRequestBuilder session(MockHttpServletRequestBuilder request) {
        return request.cookie(new Cookie(AuthController.SESSION_COOKIE, "faculty-session"))
                .header("X-Request-Id", "lecture-request");
    }

    private ObjectNode body() {
        return json.createObjectNode()
                .put("managementItemCode", "FR-031")
                .put("achievementDate", "2025-04-10")
                .put("achievementContent", "수업 개선")
                .put("academicYear", 2025)
                .put("semester", 1);
    }

    private CurrentUser user(Long id, List<String> roles) {
        return new CurrentUser(id, "faculty", "E" + id, "교원", roles, List.of());
    }

    private LectureImprovementRow row(Long owner, String status, String content, String year, Integer semester) {
        return new LectureImprovementRow(
                82L, "LI-test", owner, "교원", "DEPT-TEST", year, "FR-031",
                LocalDate.parse("2025-04-10"), status, content, 2025, semester, List.of(),
                LocalDateTime.parse("2025-04-10T09:00:00"), LocalDateTime.parse("2025-04-10T09:00:00"));
    }
}
