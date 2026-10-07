package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Real controller and service with database-free mapper boundaries; SQL/runtime verified separately by runner. */
@WebMvcTest(EmploymentRateImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EducationAchievementResponseAdvice.class, EmploymentRateImprovementService.class})
class EmploymentRateImprovementApiTest {
    private static final String PATH = "/api/business/employment-rate-improvements";
    private static final String BODY = """
            {"managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT","achievementDate":"2026-04-10",
             "specialLectureStartDate":"2026-04-01","specialLectureEndDate":"2026-04-10",
             "mockExamQuestionPeriod":"4월","attachmentIds":[]}
            """;
    @Autowired MockMvc mvc;
    @MockBean EmploymentRateImprovementMapper mapper;
    @MockBean EducationAchievementGuardMapper guard;
    private Map<String, Object> row;

    @BeforeEach
    void setup() {
        row = new LinkedHashMap<>();
        row.put("achievementId", 81L);
        row.put("managementNo", "ERI-TEST");
        row.put("teacherUserId", 101L);
        row.put("teacherName", "교원");
        row.put("organizationCode", "KNUE-DEPT-COMP");
        row.put("evaluationYear", "2026");
        row.put("managementItemCode", "EMPLOYMENT_RATE_IMPROVEMENT");
        row.put("achievementDate", LocalDate.parse("2026-04-10"));
        row.put("certificationStatus", "DRAFT");
        row.put("attachmentRef", "[]");
        row.put("achievementDetail", "{}");
        row.put("specialLectureStartDate", LocalDate.parse("2026-04-01"));
        row.put("specialLectureEndDate", LocalDate.parse("2026-04-10"));
        row.put("mockExamQuestionPeriod", "4월");
        when(mapper.find(eq(81L), anyBoolean())).thenAnswer(call -> new LinkedHashMap<>(row));
        when(mapper.inScope(eq(81L), anyLong(), anyList())).thenReturn(1);
        when(mapper.list(any(), anyLong(), anyList())).thenReturn(List.of(row));
        when(mapper.count(any(), anyLong(), anyList())).thenReturn(1L);
        when(mapper.organizations(anyLong())).thenReturn(List.of("KNUE-DEPT-COMP"));
        when(mapper.years("KNUE-DEPT-COMP")).thenReturn(List.of("2026"));
        when(mapper.managementItems(any())).thenReturn(List.of(Map.of(
                "managementItemCode", "EMPLOYMENT_RATE_IMPROVEMENT", "teacherEditableYn", "Y",
                "managementItemName", "취업률 제고", "evaluationYear", "2026", "requiredYn", "Y", "dataType", "TEXT")));
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.insertHeader(anyMap())).thenAnswer(call -> {
            Map<String, Object> insert = call.getArgument(0);
            insert.put("achievementId", 81L);
            return 1;
        });
    }

    private CurrentUser user(String... roles) {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of(roles), List.of());
    }

    @Test
    void contractIsLoadedFromClasspath() throws Exception {
        try (var input = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThat(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .contains("operationId: listEmploymentRateImprovements")
                    .contains("operationId: getEmploymentRateImprovement")
                    .contains("operationId: createEmploymentRateImprovement")
                    .contains("operationId: updateEmploymentRateImprovement");
        }
    }

    @Test
    void filteredMultiRoleListAndCountReceiveExactlyTheSameCriteriaAndScopes() throws Exception {
        mvc.perform(get(PATH).requestAttr("currentUser", user("R01", "R02"))
                        .param("managementNo", " ERI-TEST ").param("pageSize", "50"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].attachmentIds").isArray())
                .andExpect(jsonPath("$.data.managementItems[0].managementItemName").value("취업률 제고"));
        var criteria = ArgumentCaptor.forClass(EmploymentRateImprovementSearchCriteria.class);
        verify(mapper).list(criteria.capture(), eq(101L), eq(List.of("R01", "R02")));
        verify(mapper).count(eq(criteria.getValue()), eq(101L), eq(List.of("R01", "R02")));
        assertThat(criteria.getValue().managementNo()).isEqualTo("ERI-TEST");
        assertThat(criteria.getValue().pageSize()).isEqualTo(50);
    }

    @Test
    void detailReturnsApprovedRequestFieldNames() throws Exception {
        mvc.perform(get(PATH + "/81").requestAttr("currentUser", user("R01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.specialLectureStartDate").value("2026-04-01"))
                .andExpect(jsonPath("$.data.achievementDate").value("2026-04-10"));
    }

    @Test
    void createPersistsHeaderBeforeDetailThenHistoryWithSameRequestId() throws Exception {
        mvc.perform(post(PATH).requestAttr("currentUser", user("R01"))
                        .header("X-Request-Id", "ERI-CREATE").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementId").value(81))
                .andExpect(jsonPath("$.meta.requestId").value("ERI-CREATE"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(anyMap());
        order.verify(mapper).insertDetail(eq(81L), any());
        order.verify(mapper).initialStatus(81L, 101L, "ERI-CREATE");
        order.verify(mapper).find(81L, false);
        verify(mapper).audit(81L, "CREATE", "specialLectureStartDate", null, "2026-04-01", 101L, "ERI-CREATE");
    }

    @Test
    void updateRetainsEvaluationYearAndAuditsEachChangedValue() throws Exception {
        when(mapper.updateDetail(eq(81L), any())).thenAnswer(call -> {
            EmploymentRateImprovementRequest body = call.getArgument(1);
            row.put("mockExamQuestionPeriod", body.mockExamQuestionPeriod());
            row.put("specialLectureEndDate", body.specialLectureEndDate());
            return 1;
        });
        when(mapper.updateHeader(eq(81L), any(), anyString(), eq(101L))).thenAnswer(call -> {
            EmploymentRateImprovementRequest body = call.getArgument(1);
            row.put("achievementDate", body.achievementDate());
            return 1;
        });
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(0);
        mvc.perform(put(PATH + "/81").requestAttr("currentUser", user("R01"))
                        .header("X-Request-Id", "ERI-UPDATE").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("4月", "5月").replace("4월", "5월").replace("2026-04-10", "2027-04-10")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        mvc.perform(get(PATH + "/81").requestAttr("currentUser", user("R01")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mockExamQuestionPeriod").value("5월"))
                .andExpect(jsonPath("$.data.achievementDate").value("2027-04-10"));
        verify(mapper, never()).years(anyString());
        verify(guard).countActiveInputPeriods("2026", 101L);
        verify(mapper).audit(81L, "UPDATE", "mockExamQuestionPeriod", "4월", "5월", 101L, "ERI-UPDATE");
        verify(mapper).audit(81L, "UPDATE", "achievementDate", "2026-04-10", "2027-04-10", 101L, "ERI-UPDATE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"R07", "R03"})
    void disallowedRolesCannotCallAnyOperation(String role) throws Exception {
        mvc.perform(get(PATH).requestAttr("currentUser", user(role))).andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/81").requestAttr("currentUser", user(role))).andExpect(status().isForbidden());
        mvc.perform(post(PATH).requestAttr("currentUser", user(role)).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isForbidden());
        mvc.perform(put(PATH + "/81").requestAttr("currentUser", user(role)).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isForbidden());
        verifyNoInteractions(mapper, guard);
    }

    @Test
    void administratorOverrideKeepsAllOperationsAdmitted() throws Exception {
        mvc.perform(get(PATH).requestAttr("currentUser", user("R09"))).andExpect(status().isOk());
        mvc.perform(get(PATH + "/81").requestAttr("currentUser", user("R09"))).andExpect(status().isOk());
        mvc.perform(post(PATH).requestAttr("currentUser", user("R09")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isOk());
        row.put("teacherUserId", 102L);
        mvc.perform(put(PATH + "/81").requestAttr("currentUser", user("R09")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isOk());
    }

    @Test
    void readOnlyRoleCannotMutate() throws Exception {
        for (var request : List.of(post(PATH), put(PATH + "/81"))) {
            mvc.perform(request.requestAttr("currentUser", user("R02")).contentType(MediaType.APPLICATION_JSON)
                    .content(BODY)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(mapper, guard);
    }

    @Test
    void missingManagementItemIsAFieldObjectForCreateAndUpdate() throws Exception {
        for (var request : List.of(post(PATH), put(PATH + "/81"))) {
            mvc.perform(request.requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"achievementDate\":\"2026-04-10\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.managementItemCode").isString());
        }
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateHeader(any(), any(), any(), any());
    }

    @Test
    void invalidDateOrderIsRejectedBeforeWrites() throws Exception {
        mvc.perform(post(PATH).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("2026-04-01", "2026-05-01")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.specialLectureEndDate").isString());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void unknownManagementItemFailsClosed() throws Exception {
        when(mapper.managementItems("2026")).thenReturn(List.of());
        mvc.perform(post(PATH).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.managementItemCode").isString());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void unverifiedAttachmentIsRejected() throws Exception {
        mvc.perform(post(PATH).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("\"attachmentIds\":[]", "\"attachmentIds\":[\"unverified\"]")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.attachmentIds").isString());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void inputPeriodConflictDoesNotWriteCreateOrUpdate() throws Exception {
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        for (var request : List.of(post(PATH), put(PATH + "/81"))) {
            mvc.perform(request.requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                    .content(BODY)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        }
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateHeader(any(), any(), any(), any());
        verify(mapper, never()).audit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void finalizedOwnerCannotCreate() throws Exception {
        when(guard.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        mvc.perform(post(PATH).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        verify(mapper, never()).insertHeader(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED", "DEPARTMENT_CONFIRMED"})
    void nonEditableStatusPreservesOriginalRow(String state) throws Exception {
        row.put("certificationStatus", state);
        Map<String, Object> original = new LinkedHashMap<>(row);
        mvc.perform(put(PATH + "/81").requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(state.equals("EVALUATION_CONFIRMED")
                        ? "CONFIRMED_DATA_LOCKED" : "INVALID_STATE_TRANSITION"));
        assertThat(row).isEqualTo(original);
        verify(mapper, never()).updateHeader(any(), any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any());
    }

    @Test
    void outOfScopeDetailAndOtherOwnerUpdateAreForbidden() throws Exception {
        when(mapper.inScope(81L, 101L, List.of("R01"))).thenReturn(0);
        mvc.perform(get(PATH + "/81").requestAttr("currentUser", user("R01"))).andExpect(status().isForbidden());
        row.put("teacherUserId", 102L);
        mvc.perform(put(PATH + "/81").requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(any(), any(), any(), any());
    }

    @Test
    void absentOrWrongTypeResourceIsNotFound() throws Exception {
        when(mapper.find(eq(999L), anyBoolean())).thenReturn(null);
        mvc.perform(get(PATH + "/999").requestAttr("currentUser", user("R01"))).andExpect(status().isNotFound());
        mvc.perform(put(PATH + "/999").requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isNotFound());
    }

    @Test
    void ambiguousOrganizationAndInputPeriodAreNotArbitrarilyChosen() throws Exception {
        when(mapper.organizations(101L)).thenReturn(List.of("A", "B"));
        mvc.perform(post(PATH).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.organizationCode").isString());
        when(mapper.organizations(101L)).thenReturn(List.of("KNUE-DEPT-COMP"));
        when(mapper.years("KNUE-DEPT-COMP")).thenReturn(List.of("2026", "2027"));
        mvc.perform(post(PATH).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.evaluationYear").isString());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void missingDateAndBadPaginationAreValidationErrors() throws Exception {
        mvc.perform(post(PATH).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"managementItemCode\":\"EMPLOYMENT_RATE_IMPROVEMENT\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.achievementDate").isString());
        mvc.perform(get(PATH).requestAttr("currentUser", user("R01")).param("pageSize", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void internalErrorsDoNotLeakSqlOrSecrets() throws Exception {
        when(mapper.find(81L, false)).thenThrow(new IllegalStateException("password=secret SQL SELECT"));
        mvc.perform(get(PATH + "/81").requestAttr("currentUser", user("R01")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password="))));
    }
}
