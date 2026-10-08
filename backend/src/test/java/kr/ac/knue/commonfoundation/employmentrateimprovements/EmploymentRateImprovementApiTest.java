package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Real controller and service/guard with DB-free persistence mocks, using the existing MVC slice setup. */
@WebMvcTest(EmploymentRateImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EmploymentRateImprovementExceptionAdvice.class,
        EmploymentRateImprovementService.class, EducationAchievementGuardService.class})
class EmploymentRateImprovementApiTest {
    private static final String PATH = "/api/business/employment-rate-improvements";
    private static final String BODY = """
            {"managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT","achievementDate":"2026-04-10",
             "specialLectureStartDate":"2026-04-10","specialLectureEndDate":"2026-04-17",
             "mockExamQuestionPeriod":"원문 출제기간","attachmentRef":"retained-file"}
            """;
    @Autowired MockMvc mvc;
    @MockBean EmploymentRateImprovementMapper mapper;
    @MockBean EducationAchievementGuardMapper guardMapper;
    private final CurrentUser faculty = user(101L, "R01");

    @BeforeEach
    void setup() {
        when(guardMapper.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guardMapper.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.allowedItem(anyLong(), anyString(), anyString())).thenReturn(1);
        when(mapper.organization(101L)).thenReturn("KNUE-DEPT-COMP");
        when(mapper.find(82L, false)).thenReturn(row(101L, "DRAFT", "2026-04-10", "原文"));
        when(mapper.find(82L, true)).thenReturn(row(101L, "DRAFT", "2026-04-10", "原文"));
        when(mapper.managementItems(anyLong(), isNull())).thenReturn(List.of("EMPLOYMENT_RATE_IMPROVEMENT"));
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            values.put("id", 82L);
            return null;
        }).when(mapper).insertHeader(anyMap());
        when(mapper.updateHeader(anyMap())).thenReturn(1);
    }

    @Test
    void createUsesGeneratedHeaderKeyBeforeDetailAndWritesAudit() throws Exception {
        mvc.perform(command(post(PATH), faculty, BODY)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementId").value(82))
                .andExpect(jsonPath("$.meta.requestId").value("trace-test"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(anyMap());
        order.verify(mapper).insertDetail(eq(82L), any());
        order.verify(mapper).statusHistory(82L, 101L, "trace-test");
        order.verify(mapper).find(82L, false);
        verify(mapper).changeHistory(82L, "CREATE", "management_item_code", null,
                "EMPLOYMENT_RATE_IMPROVEMENT", 101L, "trace-test");
    }

    @Test
    void updatePreservesYearWarnsAndAuditsEveryChangedInput() throws Exception {
        when(mapper.find(82L, false)).thenReturn(row(101L, "DRAFT", "2027-01-10", "changed"));
        when(guardMapper.countEvaluationDatePeriods("2026", 101L, LocalDate.parse("2027-01-10"))).thenReturn(0);
        mvc.perform(command(put(PATH + "/82"), faculty,
                BODY.replace("2026-04-10", "2027-01-10").replace("2026-04-17", "2027-01-17")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        verify(guardMapper).countActiveInputPeriods("2026", 101L);
        verify(mapper).changeHistory(82L, "UPDATE", "achievement_date", "2026-04-10",
                "2027-01-10", 101L, "trace-test");
        verify(mapper).changeHistory(82L, "UPDATE", "mock_exam_question_period", "原文",
                "changed", 101L, "trace-test");
        verify(mapper).changeHistory(82L, "UPDATE", "special_lecture_start_date", "2026-04-10",
                "2027-01-10", 101L, "trace-test");
        verify(mapper).changeHistory(82L, "UPDATE", "special_lecture_end_date", "2026-04-17",
                "2027-01-17", 101L, "trace-test");
        ArgumentCaptor<Map<String, Object>> values = ArgumentCaptor.forClass(Map.class);
        verify(mapper).updateHeader(values.capture());
        assertThat(values.getValue().get("year")).isEqualTo("2026");
    }

    @Test
    void filteredListAndCountReceiveSameScopeAndCriteria() throws Exception {
        when(mapper.list(any(), eq(101L), anyList())).thenReturn(List.of(row(102L, "DRAFT", "2026-04-10", "text")));
        when(mapper.count(any(), eq(101L), anyList())).thenReturn(1L);
        CurrentUser multi = user(101L, "R01", "R02", "R04");
        mvc.perform(get(PATH).requestAttr("currentUser", multi)
                        .param("managementItemCode", "EMPLOYMENT_RATE_IMPROVEMENT")
                        .param("achievementStatus", "DRAFT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].teacherUserId").value(102));
        var criteria = ArgumentCaptor.forClass(EmploymentRateImprovementSearchCriteria.class);
        verify(mapper).list(criteria.capture(), eq(101L), eq(multi.roles()));
        verify(mapper).count(eq(criteria.getValue()), eq(101L), eq(multi.roles()));
        assertThat(criteria.getValue().managementItemCode()).isEqualTo("EMPLOYMENT_RATE_IMPROVEMENT");
    }

    @Test
    void ownDetailSucceeds() throws Exception {
        mvc.perform(get(PATH + "/82").requestAttr("currentUser", faculty))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementId").value(82));
    }

    @Test
    void multipleRolesUseUnionForDetail() throws Exception {
        when(mapper.find(82L, false)).thenReturn(row(102L, "DRAFT", "2026-04-10", "text"));
        when(guardMapper.countCertificationScope(101L, 102L)).thenReturn(1);
        mvc.perform(get(PATH + "/82").requestAttr("currentUser", user(101L, "R01", "R02", "R04")))
                .andExpect(status().isOk());
    }

    @Test
    void departmentScopeAllowsDetail() throws Exception {
        when(mapper.find(82L, false)).thenReturn(row(102L, "DRAFT", "2026-04-10", "text"));
        when(guardMapper.countSharedActiveOrganization(101L, 102L)).thenReturn(1);
        mvc.perform(get(PATH + "/82").requestAttr("currentUser", user(101L, "R02")))
                .andExpect(status().isOk());
    }

    @Test
    void outOfScopeDetailForbidden() throws Exception {
        when(mapper.find(82L, false)).thenReturn(row(102L, "DRAFT", "2026-04-10", "text"));
        mvc.perform(get(PATH + "/82").requestAttr("currentUser", faculty)).andExpect(status().isForbidden());
    }

    @Test
    void otherOwnerUpdateForbiddenWithoutMutation() throws Exception {
        when(mapper.find(82L, true)).thenReturn(row(102L, "DRAFT", "2026-04-10", "text"));
        mvc.perform(command(put(PATH + "/82"), faculty, BODY)).andExpect(status().isForbidden());
        noWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = {"R07", "R02", "R04"})
    void disallowedWriteRoleForbidden(String role) throws Exception {
        mvc.perform(command(post(PATH), user(107L, role), BODY)).andExpect(status().isForbidden());
        mvc.perform(command(put(PATH + "/82"), user(107L, role), BODY)).andExpect(status().isForbidden());
        noWrites();
    }

    @Test
    void r07CannotListOrGet() throws Exception {
        mvc.perform(get(PATH).requestAttr("currentUser", user(107L, "R07"))).andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/82").requestAttr("currentUser", user(107L, "R07")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(mapper);
    }

    @Test
    void administratorBypassIsFeatureLocalAndStillGuarded() throws Exception {
        CurrentUser admin = user(101L, "R09");
        mvc.perform(get(PATH + "/82").requestAttr("currentUser", admin)).andExpect(status().isOk());
        mvc.perform(command(post(PATH), admin, BODY)).andExpect(status().isOk());
        when(guardMapper.countActiveInputPeriods("2026", 101L)).thenReturn(0);
        mvc.perform(command(put(PATH + "/82"), admin, BODY)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
    }

    @Test
    void anonymousRequestsNeedAuthentication() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH + "/82")).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(PATH + "/82").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingManagementItemReturnsObjectFieldsOnBothCommands() throws Exception {
        String invalid = BODY.replace("EMPLOYMENT_RATE_IMPROVEMENT", "");
        for (var request : List.of(post(PATH), put(PATH + "/82"))) {
            mvc.perform(command(request, faculty, invalid)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.managementItemCode").isString())
                    .andExpect(jsonPath("$.meta.requestId").value("trace-test"));
        }
        noWrites();
    }

    @Test
    void missingDateReturnsValidationError() throws Exception {
        mvc.perform(command(post(PATH), faculty, "{\"managementItemCode\":\"EMPLOYMENT_RATE_IMPROVEMENT\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.achievementDate").isString());
        noWrites();
    }

    @Test
    void reversedSpecialLectureDatesRejected() throws Exception {
        mvc.perform(command(post(PATH), faculty, BODY.replace("2026-04-17", "2026-04-09")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.specialLectureEndDate").isString());
        noWrites();
    }

    @Test
    void incompleteSpecialLectureDatesRejected() throws Exception {
        mvc.perform(command(post(PATH), faculty, BODY.replace("\"2026-04-17\"", "null")))
                .andExpect(status().isBadRequest());
        noWrites();
    }

    @Test
    void unknownManagementItemRejected() throws Exception {
        when(mapper.allowedItem(anyLong(), anyString(), anyString())).thenReturn(0);
        mvc.perform(command(post(PATH), faculty, BODY)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.managementItemCode").isString());
        noWrites();
    }

    @Test
    void inactiveInputPeriodRejectsCreateAndUpdate() throws Exception {
        when(guardMapper.countActiveInputPeriods("2026", 101L)).thenReturn(0);
        for (var request : List.of(post(PATH), put(PATH + "/82"))) {
            mvc.perform(command(request, faculty, BODY)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        }
        noWrites();
    }

    @Test
    void evaluationFinalizationRejectsBeforeMutation() throws Exception {
        when(guardMapper.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        mvc.perform(command(put(PATH + "/82"), faculty, BODY)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        noWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = {"EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED", "DEPARTMENT_CONFIRMED"})
    void nonEditableHeaderUnchanged(String state) throws Exception {
        var original = row(101L, state, "2026-04-10", "unchanged");
        when(mapper.find(82L, true)).thenReturn(original);
        mvc.perform(command(put(PATH + "/82"), faculty, BODY)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(state.equals("EVALUATION_CONFIRMED")
                        ? "CONFIRMED_DATA_LOCKED" : "INVALID_STATE_TRANSITION"));
        noWrites();
        assertThat(mapper.find(82L, true)).isEqualTo(original);
    }

    @Test
    void notFoundGetAndUpdate() throws Exception {
        mvc.perform(get(PATH + "/999").requestAttr("currentUser", faculty)).andExpect(status().isNotFound());
        mvc.perform(command(put(PATH + "/999"), faculty, BODY)).andExpect(status().isNotFound());
    }

    @Test
    void invalidPaginationRejected() throws Exception {
        mvc.perform(get(PATH).requestAttr("currentUser", faculty).param("pageSize", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonDoesNotLeakInternalDetails() throws Exception {
        mvc.perform(command(post(PATH), faculty, "{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.message").value("입력 형식이 올바르지 않습니다."));
    }

    @Test
    void unexpectedPersistenceFailureIsSanitized() throws Exception {
        when(mapper.find(82L, false)).thenThrow(new IllegalStateException("secret jdbc password SQL"));
        mvc.perform(get(PATH + "/82").requestAttr("currentUser", faculty))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.message").value("처리 중 오류가 발생했습니다. 관리자에게 문의하세요."));
    }

    private MockHttpServletRequestBuilder command(
            MockHttpServletRequestBuilder request, CurrentUser actor, String body) {
        return request.requestAttr("currentUser", actor).header("X-Request-Id", "trace-test")
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static CurrentUser user(Long id, String... roles) {
        return new CurrentUser(id, "faculty", "E0101", "교원", List.of(roles), List.of());
    }

    private EmploymentRateImprovementRow row(Long owner, String state, String date, String period) {
        LocalDate value = LocalDate.parse(date);
        return new EmploymentRateImprovementRow(82L, owner, "KNUE-DEPT-COMP", "2026",
                "EMPLOYMENT_RATE_IMPROVEMENT", value, state, value, value.plusDays(7), period, "retained-file");
    }

    private void noWrites() {
        verify(mapper, never()).insertHeader(anyMap());
        verify(mapper, never()).updateHeader(anyMap());
        verify(mapper, never()).insertDetail(anyLong(), any());
        verify(mapper, never()).updateDetail(anyLong(), any());
        verify(mapper, never()).changeHistory(any(), any(), any(), any(), any(), any(), any());
    }
}
