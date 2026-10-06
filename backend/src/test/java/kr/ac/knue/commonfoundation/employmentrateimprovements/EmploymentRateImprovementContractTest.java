package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
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
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Real HTTP controller/service/guard/filter path; SQL execution and real menu resolution remain integration checks. */
class EmploymentRateImprovementContractTest {
    private static final String BASE = "/api/business/employment-rate-improvements";
    private static final Cookie SESSION = new Cookie(AuthController.SESSION_COOKIE, "fr029-contract-session");
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private EmploymentRateImprovementMapper mapper;
    private EducationAchievementGuardMapper guardMapper;
    private FunctionPermissionService functions;
    private AuthService auth;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mapper = Mockito.mock(EmploymentRateImprovementMapper.class);
        guardMapper = Mockito.mock(EducationAchievementGuardMapper.class);
        functions = Mockito.mock(FunctionPermissionService.class);
        auth = Mockito.mock(AuthService.class);
        var menus = Mockito.mock(EffectivePermissionService.class);
        when(menus.canAccess(anyLong(), any(), anyString())).thenReturn(true);
        when(auth.currentUser(SESSION.getValue())).thenReturn(user(101L, List.of("R01")));
        when(guardMapper.countActiveInputPeriods("2025", 101L)).thenReturn(1);
        when(guardMapper.countEvaluationDatePeriods(eq("2025"), eq(101L), any())).thenReturn(1);
        when(mapper.organizations(101L)).thenReturn(List.of("KNUE-DEPT-COMP"));
        when(mapper.managementItems("FR-029", "2025")).thenReturn(List.of(Map.of(
                "requiredYn", "Y", "teacherEditableYn", "Y", "dataType", "TEXT")));
        var service = new EmploymentRateImprovementService(
                mapper, new EducationAchievementGuardService(guardMapper), functions, json);
        mvc = MockMvcBuilders.standaloneSetup(new EmploymentRateImprovementController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
                .addFilters(new AuthenticationFilter(auth, menus, json))
                .build();
    }

    @Test
    void listUsesNormalizedFiltersAndAllRolesForRowsAndCount() throws Exception {
        var roles = List.of("R01", "R02", "R04");
        when(auth.currentUser(SESSION.getValue())).thenReturn(user(101L, roles));
        when(mapper.list(any(), eq(101L), eq(roles))).thenReturn(List.of(row(202L, "DRAFT", "saved")));
        when(mapper.count(any(), eq(101L), eq(roles))).thenReturn(1L);
        mvc.perform(get(BASE).cookie(SESSION).param("teacherName", " 교원 ").param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].teacherUserId").value(202))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(50));
        var criteria = ArgumentCaptor.forClass(EmploymentRateImprovementSearchCriteria.class);
        verify(mapper).list(criteria.capture(), eq(101L), eq(roles));
        assertThat(criteria.getValue().teacherName()).isEqualTo("교원");
        verify(mapper).count(eq(criteria.getValue()), eq(101L), eq(roles));
    }

    @Test
    void createInsertsHeaderBeforeDetailReadbackAndBothHistories() throws Exception {
        var saved = row(101L, "DRAFT", "saved");
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            values.put("id", 77L);
            return null;
        }).when(mapper).insertHeader(any());
        when(mapper.find(77L)).thenReturn(saved);
        mvc.perform(post(BASE).cookie(SESSION).header("X-Request-Id", "fr029-create")
                .contentType(MediaType.APPLICATION_JSON).content(body("saved", "2025-04-10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.requestId").value("fr029-create"))
                .andExpect(jsonPath("$.data.achievement.achievementId").value(77))
                .andExpect(jsonPath("$.data.achievement.specialLectureStartDate").isNotEmpty());
        var order = Mockito.inOrder(mapper);
        order.verify(mapper).insertHeader(any());
        order.verify(mapper).insertDetail(eq(77L), any());
        order.verify(mapper).statusHistory(77L, 101L);
        order.verify(mapper).find(77L);
        order.verify(mapper).changeHistory(eq(77L), eq("CREATE"), isNull(), anyString(), eq(101L),
                eq("fr029-create"));
    }

    @Test
    void updateKeepsYearAndWritesFullBeforeAfterThenDetailReturnsNewValues() throws Exception {
        var before = row(101L, "DRAFT", "old");
        var after = row(101L, "DRAFT", "new");
        when(mapper.lock(77L)).thenReturn(before);
        when(mapper.find(77L)).thenReturn(after);
        when(mapper.visible(77L, 101L, List.of("R01"))).thenReturn(1);
        when(mapper.updateHeader(eq(77L), any(), eq(101L))).thenReturn(1);
        when(guardMapper.countEvaluationDatePeriods(eq("2025"), eq(101L), any())).thenReturn(0);
        mvc.perform(put(BASE + "/77").cookie(SESSION).header("X-Request-Id", "fr029-update")
                .contentType(MediaType.APPLICATION_JSON).content(body("new", "2026-04-10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2025"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        verify(guardMapper).countActiveInputPeriods("2025", 101L);
        verify(mapper).updateDetail(eq(77L), any());
        verify(mapper).changeHistory(77L, "UPDATE", json.writeValueAsString(before),
                json.writeValueAsString(after), 101L, "fr029-update");
        mvc.perform(get(BASE + "/77").cookie(SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mockExamQuestionPeriod").value("new"));
    }

    @Test
    void confirmedUpdateHas409AndNoWrites() throws Exception {
        when(mapper.lock(77L)).thenReturn(row(101L, "EVALUATION_CONFIRMED", "original"));
        mvc.perform(put(BASE + "/77").cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content(body("changed", "2025-04-10")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        verify(mapper, never()).updateHeader(any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any());
        verify(mapper, never()).changeHistory(any(), any(), any(), any(), any(), any());
    }

    @Test
    void periodPrecedesRowLockAndFinalizationAlsoBlocksCreate() throws Exception {
        when(mapper.lock(77L)).thenReturn(row(101L, "EVALUATION_CONFIRMED", "original"));
        when(guardMapper.countActiveInputPeriods("2025", 101L)).thenReturn(0);
        mvc.perform(put(BASE + "/77").cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content(body("changed", "2025-04-10")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        when(guardMapper.countActiveInputPeriods("2025", 101L)).thenReturn(1);
        when(guardMapper.countEvaluationConfirmations(101L, "2025")).thenReturn(1);
        mvc.perform(post(BASE).cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content(body("changed", "2025-04-10")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void rejectsR07R09AnonymousAndOtherOwnerWithoutChangingOriginal() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        for (String role : List.of("R07", "R09")) {
            when(auth.currentUser(SESSION.getValue())).thenReturn(user(101L, List.of(role)));
            mvc.perform(post(BASE).cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                    .content(body("new", "2025-04-10"))).andExpect(status().isForbidden());
        }
        when(auth.currentUser(SESSION.getValue())).thenReturn(user(101L, List.of("R01")));
        when(mapper.lock(77L)).thenReturn(row(202L, "DRAFT", "original"));
        mvc.perform(put(BASE + "/77").cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content(body("new", "2025-04-10"))).andExpect(status().isForbidden());
        when(mapper.find(77L)).thenReturn(row(202L, "DRAFT", "original"));
        mvc.perform(get(BASE + "/77").cookie(SESSION)).andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(any(), any(), any());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void validationAndMissingUpdateCannotInsertAndForgedStatusIsRejected() throws Exception {
        mvc.perform(post(BASE).cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content("{\"achievementDate\":\"2025-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        mvc.perform(put(BASE + "/999").cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content(body("new", "2025-04-10"))).andExpect(status().isNotFound());
        mvc.perform(post(BASE).cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content(body("new", "2025-04-10").replace("}", ",\"achievementStatus\":\"DELETED\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("요청 필드 또는 날짜 형식이 올바르지 않습니다."));
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void invalidLecturePeriodAndDuplicateManagementCodeAndUnverifiedAttachmentAreRejected() throws Exception {
        mvc.perform(post(BASE).cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content(body("new", "2025-04-10").replace("2025-04-03", "2025-03-01")))
                .andExpect(status().isBadRequest());
        var item = Map.<String, Object>of("requiredYn", "Y", "teacherEditableYn", "Y", "dataType", "TEXT");
        when(mapper.managementItems("FR-029", "2025")).thenReturn(List.of(item, item));
        mvc.perform(post(BASE).cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content(body("new", "2025-04-10"))).andExpect(status().isBadRequest());
        when(mapper.managementItems("FR-029", "2025")).thenReturn(List.of(item));
        mvc.perform(post(BASE).cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content(body("new", "2025-04-10").replace("}", ",\"attachmentIds\":[\"../unsafe\"]}")))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void malformedManagementDateReturnsFieldValidationWithoutWrites() throws Exception {
        when(mapper.managementItems("FR-029", "2025")).thenReturn(List.of(Map.of(
                "requiredYn", "Y", "teacherEditableYn", "Y", "dataType", "DATE")));
        mvc.perform(post(BASE).cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content(body("not-a-date", "2025-04-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'mockExamQuestionPeriod')]").isNotEmpty());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void functionDenialAndSubmittedStatusAreIndependentOfMenuAccess() throws Exception {
        Mockito.doThrow(new kr.ac.knue.commonfoundation.common.api.ForbiddenException())
                .when(functions).evaluate(any());
        mvc.perform(get(BASE).cookie(SESSION)).andExpect(status().isForbidden());
        Mockito.reset(functions);
        when(mapper.lock(77L)).thenReturn(row(101L, "SUBMITTED", "original"));
        mvc.perform(put(BASE + "/77").cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content(body("changed", "2025-04-10")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STATUS_NOT_EDITABLE"));
        verify(mapper, never()).updateHeader(any(), any(), any());
    }

    @Test
    void missingDetailAndUpdateValidationAndSensitiveFailureHaveStableEnvelopes() throws Exception {
        mvc.perform(get(BASE + "/999").cookie(SESSION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mvc.perform(put(BASE + "/77").cookie(SESSION).contentType(MediaType.APPLICATION_JSON)
                .content("{\"achievementDate\":\"2025-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        when(mapper.find(77L)).thenThrow(new IllegalStateException("SQL password=private-token"));
        String error = mvc.perform(get(BASE + "/77").cookie(SESSION))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andReturn().getResponse().getContentAsString();
        assertThat(error).doesNotContain("private-token", "SQL", "IllegalStateException");
    }

    @Test
    void contractFixtureIsClasspathAccessible() {
        assertThat(new ClassPathResource("contracts/openapi.yaml").exists()).isTrue();
    }

    private String body(String period, String date) throws Exception {
        return json.writeValueAsString(new EmploymentRateImprovementRequest(
                "FR-029", LocalDate.parse(date), LocalDate.parse("2025-04-01"),
                LocalDate.parse("2025-04-03"), period, null));
    }

    private CurrentUser user(Long id, List<String> roles) {
        return new CurrentUser(id, "faculty", "E0101", "교원", roles, List.of());
    }

    private EmploymentRateImprovementRow row(Long owner, String state, String period) {
        return new EmploymentRateImprovementRow(
                77L, "ERI-test", owner, "교원", "KNUE-DEPT-COMP", "2025", "FR-029",
                LocalDate.parse("2025-04-10"), state, LocalDate.parse("2025-04-01"),
                LocalDate.parse("2025-04-03"), period, List.of(),
                LocalDateTime.parse("2025-04-10T10:00:00"), LocalDateTime.parse("2025-04-10T10:00:00"));
    }
}
