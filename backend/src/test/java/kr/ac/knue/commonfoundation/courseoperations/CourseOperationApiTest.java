package kr.ac.knue.commonfoundation.courseoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementRequestFilter;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementResponseAdvice;
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

/** Real HTTP/controller/service rules with only database adapters mocked; SQL runs in the API runner. */
@WebMvcTest(CourseOperationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CourseOperationService.class, GlobalExceptionHandler.class, EducationAchievementResponseAdvice.class})
class CourseOperationApiTest {
    private static final String URL = "/api/business/course-operations";
    private static final String BODY = """
            {"managementItemCode":"COURSE_OPERATION","achievementDate":"2026-04-10",
             "performanceDetails":"새 실적내역","attachmentIds":[]}
            """;
    @Autowired MockMvc mvc;
    @MockBean CourseOperationMapper mapper;
    @MockBean EducationAchievementGuardMapper guard;

    CurrentUser user(String... roles) {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of(roles), List.of());
    }

    CourseOperationRow row(Long owner, String state, String details, String date) {
        return new CourseOperationRow(82L, "CO-TEST", owner, "faculty", "KNUE-DEPT-COMP", "2026",
                "COURSE_OPERATION", LocalDate.parse(date), state, details, List.of());
    }

    @BeforeEach
    void setup() {
        when(mapper.organizations(101L)).thenReturn(List.of("KNUE-DEPT-COMP"));
        when(mapper.years("KNUE-DEPT-COMP")).thenReturn(List.of("2026"));
        when(mapper.managementItems()).thenReturn(List.of(new CourseOperationManagementItem(
                "COURSE_OPERATION", "강좌 운영", "2026", "Y", "TEXT", "Y")));
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.visible(anyLong(), anyLong(), any())).thenReturn(1);
        when(mapper.lock(82L)).thenReturn(row(101L, "DRAFT", "이전 내역", "2026-03-15"));
        when(mapper.find(82L)).thenReturn(row(101L, "DRAFT", "새 실적내역", "2026-04-10"));
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            values.put("id", 82L);
            return null;
        }).when(mapper).insertHeader(any());
    }

    MockHttpServletRequestBuilder request(MockHttpServletRequestBuilder builder, CurrentUser actor) {
        return builder.requestAttr("currentUser", actor)
                .requestAttr(EducationAchievementRequestFilter.REQUEST_ID_ATTRIBUTE, "REQ-COURSE-TEST")
                .header("X-Request-Id", "REQ-COURSE-TEST");
    }

    @Test
    void createThenListAndDetailMatchAndAuditUsesGeneratedKey() throws Exception {
        mvc.perform(request(post(URL), user("R01")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(82))
                .andExpect(jsonPath("$.data.achievement.performanceDetails").value("새 실적내역"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-COURSE-TEST"));
        var ordered = inOrder(mapper);
        ordered.verify(mapper).insertHeader(any());
        ordered.verify(mapper).insertDetail(82L, "새 실적내역");
        ordered.verify(mapper).initialStatus(82L, 101L, "REQ-COURSE-TEST");
        verify(mapper).history(82L, "CREATE", "performanceDetails", null, "새 실적내역", 101L, "REQ-COURSE-TEST");
        CourseOperationRow savedRow = mapper.find(82L);
        when(mapper.list(any(), eq(101L), any())).thenReturn(List.of(savedRow));
        when(mapper.count(any(), eq(101L), any())).thenReturn(1L);
        mvc.perform(request(get(URL), user("R01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].performanceDetails").value("새 실적내역"));
        mvc.perform(request(get(URL + "/82"), user("R01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceDetails").value("새 실적내역"));
    }

    @Test
    void updateAuditsEveryChangedValueAndRetainsHeaderYearForOutsideDate() throws Exception {
        when(mapper.find(82L)).thenReturn(row(101L, "DRAFT", "새 실적내역", "2025-12-31"));
        when(guard.countEvaluationDatePeriods(eq("2026"), eq(101L), any())).thenReturn(0);
        mvc.perform(request(put(URL + "/82"), user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("2026-04-10", "2025-12-31")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.achievement.achievementDate").value("2025-12-31"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        verify(guard).countActiveInputPeriods("2026", 101L);
        verify(mapper).updateDetail(82L, "새 실적내역");
        verify(mapper).history(82L, "UPDATE", "achievementDate", "2026-03-15", "2025-12-31",
                101L, "REQ-COURSE-TEST");
        verify(mapper).history(82L, "UPDATE", "performanceDetails", "이전 내역", "새 실적내역",
                101L, "REQ-COURSE-TEST");
        ArgumentCaptor<Map<String, Object>> values = ArgumentCaptor.forClass(Map.class);
        verify(mapper).updateHeader(values.capture());
        assertThat(values.getValue()).doesNotContainKeys("year", "organization", "teacherUserId");
        mvc.perform(request(get(URL + "/82"), user("R01")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementDate").value("2025-12-31"));
    }

    @Test
    void filtersAndUnionRolesAreIdenticalForListAndCount() throws Exception {
        when(mapper.count(any(), eq(101L), eq(List.of("R01", "R02", "R04")))).thenReturn(3L);
        mvc.perform(request(get(URL).param("managementNo", " CO-TEST ").param("page", "1"),
                        user("R01", "R02", "R04")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(3));
        var expected = new CourseOperationSearchCriteria(1, 20, 20, "CO-TEST", null, null, null);
        verify(mapper).list(expected, 101L, List.of("R01", "R02", "R04"));
        verify(mapper).count(expected, 101L, List.of("R01", "R02", "R04"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"R01", "R02", "R04", "R09"})
    void allowedReadersReachBothReadOperations(String role) throws Exception {
        mvc.perform(request(get(URL), user(role))).andExpect(status().isOk());
        mvc.perform(request(get(URL + "/82"), user(role))).andExpect(status().isOk());
    }

    @Test
    void administratorOverrideAdmitsCreateAndOtherOwnerUpdate() throws Exception {
        mvc.perform(request(post(URL), user("R09")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
        when(mapper.lock(82L)).thenReturn(row(202L, "DRAFT", "다른 교원", "2026-03-15"));
        mvc.perform(request(put(URL + "/82"), user("R09"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"R02", "R04", "R07"})
    void nonWritersCannotCreateOrUpdate(String role) throws Exception {
        mvc.perform(request(post(URL), user(role)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(request(put(URL + "/82"), user(role)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void r07CannotReadEitherOperation() throws Exception {
        mvc.perform(request(get(URL), user("R07"))).andExpect(status().isForbidden());
        mvc.perform(request(get(URL + "/82"), user("R07"))).andExpect(status().isForbidden());
        verify(mapper, never()).find(any());
    }

    @Test
    void anonymousIsUnauthenticated() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void missingPerformanceDetailsIsFieldObjectOnBothMutations() throws Exception {
        String invalid = BODY.replace("\"performanceDetails\":\"새 실적내역\",", "");
        for (var builder : List.of(post(URL), put(URL + "/82"))) {
            mvc.perform(request(builder, user("R01")).contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.performanceDetails").isString());
        }
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void missingManagementItemAndDateAreValidated() throws Exception {
        mvc.perform(request(post(URL), user("R01")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"performanceDetails\":\"내용\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.managementItemCode").isString())
                .andExpect(jsonPath("$.error.fields.achievementDate").isString());
    }

    @Test
    void unknownManagementItemIsRejected() throws Exception {
        mvc.perform(request(post(URL), user("R01")).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("COURSE_OPERATION", "UNKNOWN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.managementItemCode").isString());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void ambiguousManagementItemIsRejected() throws Exception {
        var item = mapper.managementItems().get(0);
        when(mapper.managementItems()).thenReturn(List.of(item, item));
        mvc.perform(request(post(URL), user("R01")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonEditableManagementItemIsForbidden() throws Exception {
        when(mapper.managementItems()).thenReturn(List.of(new CourseOperationManagementItem(
                "COURSE_OPERATION", "항목", "2026", "Y", "TEXT", "N")));
        mvc.perform(request(post(URL), user("R01")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void ambiguousOrganizationIsRejected() throws Exception {
        when(mapper.organizations(101L)).thenReturn(List.of("ORG-A", "ORG-B"));
        mvc.perform(request(post(URL), user("R01")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.organizationCode").isString());
    }

    @Test
    void ambiguousPeriodIsRejected() throws Exception {
        when(mapper.years("KNUE-DEPT-COMP")).thenReturn(List.of("2026", "2027"));
        mvc.perform(request(post(URL), user("R01")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.evaluationYear").isString());
    }

    @Test
    void closedPeriodCreateAndUpdateDoNotWrite() throws Exception {
        when(mapper.years("KNUE-DEPT-COMP")).thenReturn(List.of());
        mvc.perform(request(post(URL), user("R01")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        when(guard.countActiveInputPeriods("2026", 101L)).thenReturn(0);
        mvc.perform(request(put(URL + "/82"), user("R01")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateHeader(any());
        verify(mapper, never()).history(any(), any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUBMITTED", "CERTIFIED", "DEPARTMENT_CONFIRMED", "EVALUATION_CONFIRMED"})
    void nonEditableStateLeavesOriginalUnchanged(String state) throws Exception {
        var original = row(101L, state, "이전 내역", "2026-03-15");
        when(mapper.lock(82L)).thenReturn(original);
        when(mapper.find(82L)).thenReturn(original);
        mvc.perform(request(put(URL + "/82"), user("R01")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(state.equals("EVALUATION_CONFIRMED")
                        ? "CONFIRMED_DATA_LOCKED" : "INVALID_STATE_TRANSITION"));
        mvc.perform(request(get(URL + "/82"), user("R01")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.performanceDetails").value("이전 내역"));
        verify(mapper, never()).updateHeader(any());
        verify(mapper, never()).updateDetail(any(), any());
    }

    @Test
    void evaluationFinalizationLocksDraft() throws Exception {
        when(guard.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        mvc.perform(request(put(URL + "/82"), user("R01")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void otherOwnerCannotUpdateEvenWithAdditionalReaderRole() throws Exception {
        when(mapper.lock(82L)).thenReturn(row(202L, "DRAFT", "원본", "2026-03-15"));
        mvc.perform(request(put(URL + "/82"), user("R01", "R02"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void outOfScopeDetailIsForbidden() throws Exception {
        when(mapper.visible(82L, 101L, List.of("R01"))).thenReturn(0);
        mvc.perform(request(get(URL + "/82"), user("R01"))).andExpect(status().isForbidden());
    }

    @Test
    void absentDeletedOrWrongTypeRowsAreNotFound() throws Exception {
        when(mapper.find(82L)).thenReturn(null);
        when(mapper.lock(82L)).thenReturn(null);
        mvc.perform(request(get(URL + "/82"), user("R01"))).andExpect(status().isNotFound());
        mvc.perform(request(put(URL + "/82"), user("R01")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void arbitraryAttachmentTokensAreNotAccepted() throws Exception {
        mvc.perform(request(post(URL), user("R01")).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("[]", "[\"invented-token\"]")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.attachmentIds").isString());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void invalidPaginationIsRejected() throws Exception {
        mvc.perform(request(get(URL).param("pageSize", "10"), user("R01"))).andExpect(status().isBadRequest());
        mvc.perform(request(get(URL).param("page", "-1"), user("R01"))).andExpect(status().isBadRequest());
    }

    @Test
    void malformedInputDoesNotLeakParserDetails() throws Exception {
        mvc.perform(request(post(URL), user("R01")).contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.body").isString());
    }
}
