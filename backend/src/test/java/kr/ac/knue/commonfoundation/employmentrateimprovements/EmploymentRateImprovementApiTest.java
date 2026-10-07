package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementConflictAdvice;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionMapper;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Real controller/service requests with DB-free persistence mocks, following the existing MVC test setup. */
@WebMvcTest(EmploymentRateImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        EducationAchievementConflictAdvice.class,
        EmploymentRateImprovementService.class
})
class EmploymentRateImprovementApiTest {
    private static final String ROOT = "/api/business/employment-rate-improvements";
    private static final String BODY = """
            {"managementItemCode":"ATTENDANCE","achievementDate":"2026-04-10",
             "specialLectureStartDate":"2026-04-01","specialLectureEndDate":"2026-04-10",
             "mockExamQuestionPeriod":"4월","attachmentIds":[]}
            """;
    @Autowired private MockMvc mvc;
    @MockBean private EmploymentRateImprovementMapper mapper;
    @MockBean private EducationAchievementGuardMapper guards;
    @MockBean private FunctionPermissionMapper permissions;
    private final CurrentUser faculty = user(101L, List.of("R01"));

    @BeforeEach
    void setup() {
        when(permissions.findByKey(anyString(), anyString(), anyString())).thenAnswer(call ->
                new FunctionPermissionRow(1L, call.getArgument(0), "실적", call.getArgument(1), "역할",
                        call.getArgument(2), "ALLOW", null, LocalDateTime.parse("2026-04-01T09:00:00")));
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guards.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.managementItems("ATTENDANCE")).thenReturn(1);
        when(mapper.organizations(anyLong(), any())).thenReturn(List.of("KNUE-DEPT-COMP"));
        when(mapper.find(8L)).thenReturn(row(101L, "DRAFT", "2026", "4월"));
        when(mapper.lock(8L)).thenReturn(row(101L, "DRAFT", "2026", "4월"));
        when(mapper.visible(eq(8L), anyLong(), any())).thenReturn(1);
        when(mapper.updateHeader(eq(8L), any(), anyString(), anyLong())).thenReturn(1);
        when(mapper.updateDetail(eq(8L), any())).thenReturn(1);
        doAnswer(call -> {
            Map<String, Object> header = call.getArgument(0);
            header.put("achievementId", 8L);
            return null;
        }).when(mapper).insertHeader(any());
    }

    @Test
    void createUsesGeneratedIdentityBeforeDetailAndWritesFullSnapshotHistory() throws Exception {
        mvc.perform(as(post(ROOT), faculty).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementId").value(8))
                .andExpect(jsonPath("$.data.achievement.specialLectureStartDate").value("2026-04-01"))
                .andExpect(jsonPath("$.data.achievement.attachmentIds").isArray())
                .andExpect(jsonPath("$.meta.requestId").value("trace"));
        var order = org.mockito.Mockito.inOrder(mapper);
        order.verify(mapper).insertHeader(any());
        order.verify(mapper).insertDetail(eq(8L), any());
        order.verify(mapper).statusHistory(8L, 101L);
        order.verify(mapper).find(8L);
        ArgumentCaptor<String> after = ArgumentCaptor.forClass(String.class);
        verify(mapper).history(eq(8L), eq("CREATE"), eq("achievement"), eq(null), after.capture(),
                eq(101L), eq("trace"));
        assertThat(after.getValue()).contains(
                "specialLectureStartDate", "mockExamQuestionPeriod", "managementItemCode");
    }

    @Test
    void updateAndDetailReturnChangedValuesWithUnchangedEvaluationYear() throws Exception {
        var updated = new EmploymentRateImprovementRow(8L, "ERI-test", 101L, "faculty", "KNUE-DEPT-COMP",
                "2026", "ATTENDANCE", LocalDate.parse("2025-12-31"), "DRAFT", "[]",
                LocalDate.parse("2026-04-01"), LocalDate.parse("2026-04-10"), "변경",
                LocalDateTime.parse("2026-04-01T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00"));
        when(mapper.find(8L)).thenReturn(updated);
        when(guards.countEvaluationDatePeriods("2026", 101L, LocalDate.parse("2025-12-31"))).thenReturn(0);
        mvc.perform(as(put(ROOT + "/8"), faculty)
                        .content(BODY.replace(
                                "\"achievementDate\":\"2026-04-10\"",
                                "\"achievementDate\":\"2025-12-31\"")
                                .replace("4월", "변경")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        mvc.perform(as(get(ROOT + "/8"), faculty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mockExamQuestionPeriod").value("변경"))
                .andExpect(jsonPath("$.data.achievementDate").value("2025-12-31"));
        verify(guards).countActiveInputPeriods("2026", 101L);
        ArgumentCaptor<String> before = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> after = ArgumentCaptor.forClass(String.class);
        verify(mapper).history(eq(8L), eq("UPDATE"), eq("achievement"), before.capture(), after.capture(),
                eq(101L), eq("trace"));
        assertThat(before.getValue()).contains("4월", "2026-04-10");
        assertThat(after.getValue()).contains("변경", "2025-12-31");
    }

    @Test
    void listPassesAllRolesAndTheSameNormalizedFilterToCount() throws Exception {
        var multi = user(101L, List.of("R01", "R02", "R04"));
        when(mapper.list(any(), eq(101L), eq(multi.roles())))
                .thenReturn(List.of(row(102L, "DRAFT", "2026", "타인")));
        when(mapper.count(any(), eq(101L), eq(multi.roles()))).thenReturn(1L);
        mvc.perform(as(get(ROOT).param("managementNo", " ERI-test "), multi))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].teacherUserId").value(102))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        var criteria = ArgumentCaptor.forClass(EmploymentRateImprovementSearchCriteria.class);
        verify(mapper).list(criteria.capture(), eq(101L), eq(multi.roles()));
        verify(mapper).count(eq(criteria.getValue()), eq(101L), eq(multi.roles()));
        assertThat(criteria.getValue().managementNo()).isEqualTo("ERI-test");
    }

    @Test
    void r07CannotReadCreateOrUpdate() throws Exception {
        var operator = user(107L, List.of("R07"));
        for (var call : List.of(get(ROOT), get(ROOT + "/8"), post(ROOT), put(ROOT + "/8"))) {
            mvc.perform(as(call, operator).content(BODY)).andExpect(status().isForbidden());
        }
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateHeader(any(), any(), any(), any());
    }

    @Test
    void r02AndR04CannotWrite() throws Exception {
        for (String role : List.of("R02", "R04")) {
            mvc.perform(as(post(ROOT), user(102L, List.of(role))).content(BODY))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void administratorOverrideStillUsesBusinessPeriodGuard() throws Exception {
        when(mapper.organizations(109L, LocalDate.parse("2026-04-10"))).thenReturn(List.of("KNUE"));
        mvc.perform(as(post(ROOT), user(109L, List.of("R09"))).content(BODY))
                .andExpect(status().isOk());
        verify(guards).countActiveInputPeriods("2026", 109L);
    }

    @Test
    void missingManagementItemOnBothWriteOperationsReturnsFieldError() throws Exception {
        for (var call : List.of(post(ROOT), put(ROOT + "/8"))) {
            mvc.perform(as(call, faculty).content("{\"achievementDate\":\"2026-04-10\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        }
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void invalidSpecialLectureRangeIs400WithoutWrite() throws Exception {
        mvc.perform(as(post(ROOT), faculty).content(BODY.replace("2026-04-01", "2026-05-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'specialLectureEndDate')]").isNotEmpty());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void activeManagementItemMustBeUnique() throws Exception {
        when(mapper.managementItems("ATTENDANCE")).thenReturn(2);
        mvc.perform(as(post(ROOT), faculty).content(BODY)).andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void ambiguousOrganizationIs400WithoutWrite() throws Exception {
        when(mapper.organizations(anyLong(), any())).thenReturn(List.of("A", "B"));
        mvc.perform(as(post(ROOT), faculty).content(BODY)).andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void nonexistentFileIsNotAcceptedAsAnAttachment() throws Exception {
        mvc.perform(as(post(ROOT), faculty).content(BODY.replace("\"attachmentIds\":[]",
                        "\"attachmentIds\":[\"unverified-token\"]")))
                .andExpect(status().isNotFound());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void confirmedRowCannotChangeAndOriginalIsPreserved() throws Exception {
        var original = row(101L, "EVALUATION_CONFIRMED", "2026", "4월");
        when(mapper.lock(8L)).thenReturn(original);
        when(mapper.find(8L)).thenReturn(original);
        mvc.perform(as(put(ROOT + "/8"), faculty).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"))
                .andExpect(jsonPath("$.meta.requestId").value("trace"));
        mvc.perform(as(get(ROOT + "/8"), faculty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mockExamQuestionPeriod").value("4월"));
        verify(mapper, never()).updateHeader(any(), any(), any(), any());
        verify(mapper, never()).history(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void finalizationBlocksCreateBeforeFunctionEvaluation() throws Exception {
        when(guards.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        mvc.perform(as(post(ROOT), faculty).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        verify(mapper, never()).insertHeader(any());
        verify(permissions, never()).findByKey(any(), any(), any());
    }

    @Test
    void inactivePeriodBlocksBothWritesWithExactCode() throws Exception {
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        for (var call : List.of(post(ROOT), put(ROOT + "/8"))) {
            mvc.perform(as(call, faculty).content(BODY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        }
        verify(mapper, never()).updateHeader(any(), any(), any(), any());
    }

    @Test
    void submittedRowCannotBeEdited() throws Exception {
        when(mapper.lock(8L)).thenReturn(row(101L, "SUBMITTED", "2026", "기존"));
        mvc.perform(as(put(ROOT + "/8"), faculty).content(BODY)).andExpect(status().isConflict());
        verify(mapper, never()).updateDetail(any(), any());
    }

    @Test
    void otherOwnerAndOutOfScopeDetailAreForbidden() throws Exception {
        when(mapper.lock(8L)).thenReturn(row(102L, "DRAFT", "2026", "타인"));
        when(mapper.visible(eq(8L), anyLong(), any())).thenReturn(0);
        mvc.perform(as(put(ROOT + "/8"), faculty).content(BODY)).andExpect(status().isForbidden());
        mvc.perform(as(get(ROOT + "/8"), faculty)).andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(any(), any(), any(), any());
    }

    @Test
    void missingDetailAndUpdateAre404NotUpsert() throws Exception {
        for (var call : List.of(get(ROOT + "/99"), put(ROOT + "/99"))) {
            mvc.perform(as(call, faculty).content(BODY)).andExpect(status().isNotFound());
        }
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void explicitFunctionDenyWinsAcrossRoles() throws Exception {
        when(permissions.findByKey(EmploymentRateImprovementService.SCREEN, "R02", "READ"))
                .thenReturn(new FunctionPermissionRow(2L, EmploymentRateImprovementService.SCREEN, "실적", "R02",
                        "학과장", "READ", "DENY", null, null));
        mvc.perform(as(get(ROOT), user(101L, List.of("R01", "R02")))).andExpect(status().isForbidden());
        verify(mapper, never()).list(any(), any(), any());
    }

    @Test
    void unauthenticatedRequestIs401() throws Exception {
        mvc.perform(get(ROOT)).andExpect(status().isUnauthorized());
    }

    @Test
    void malformedDatesAndPathIdsAre400AndDoNotLeakParserDetails() throws Exception {
        var result = mvc.perform(as(post(ROOT), faculty).content(BODY.replace("2026-04-10", "not-a-date")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.meta.requestId").value("trace"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("java.time", "not-a-date", "Exception", "SQL");
        mvc.perform(as(get(ROOT + "/not-a-number"), faculty))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void approvedContractIsLoadedOnlyFromTheBuildClasspath() throws Exception {
        try (var input = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThat(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .contains(
                            "operationId: listEmploymentRateImprovements",
                            "operationId: getEmploymentRateImprovement",
                            "operationId: createEmploymentRateImprovement",
                            "operationId: updateEmploymentRateImprovement");
        }
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, CurrentUser user) {
        return request.requestAttr("currentUser", user)
                .cookie(new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"))
                .header("X-Request-Id", " trace ")
                .contentType(MediaType.APPLICATION_JSON);
    }

    private static CurrentUser user(Long id, List<String> roles) {
        return new CurrentUser(id, "faculty", "E" + id, "교원", roles, List.of());
    }

    private static EmploymentRateImprovementRow row(Long owner, String status, String year, String period) {
        return new EmploymentRateImprovementRow(8L, "ERI-test", owner, "faculty", "KNUE-DEPT-COMP",
                year, "ATTENDANCE", LocalDate.parse("2026-04-10"), status, "[]",
                LocalDate.parse("2026-04-01"), LocalDate.parse("2026-04-10"), period,
                LocalDateTime.parse("2026-04-01T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}
