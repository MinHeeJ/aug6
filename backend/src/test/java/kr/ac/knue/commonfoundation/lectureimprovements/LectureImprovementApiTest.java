package kr.ac.knue.commonfoundation.lectureimprovements;

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
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementResponseAdvice;
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

/** Real controller/service behavior with DB-free mapper ports, using the neighboring MVC setup. */
@WebMvcTest(LectureImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EducationAchievementResponseAdvice.class, LectureImprovementService.class})
class LectureImprovementApiTest {
    @Autowired MockMvc mvc;
    @MockBean LectureImprovementMapper mapper;
    @MockBean EducationAchievementGuardMapper guards;
    final CurrentUser faculty = actor(101L, "R01");
    final String body = """
            {"managementItemCode":"LECTURE_IMPROVEMENT","achievementDate":"2025-12-31",
             "achievementContent":"강의 개선 보고서","academicYear":2025,"semester":2,"attachmentIds":[]}
            """;

    @BeforeEach
    void setup() {
        when(mapper.organizations(anyLong())).thenReturn(List.of("DEPT"));
        when(mapper.evaluationYears(anyString())).thenReturn(List.of("2026"));
        when(mapper.managementItems()).thenReturn(List.of(new LectureImprovementManagementItem(
                "LECTURE_IMPROVEMENT", "강의개선", "Y", "TEXT", "Y", "2026")));
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guards.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.find(501L, false)).thenReturn(row(101L, "DRAFT"));
        when(mapper.find(501L, true)).thenReturn(row(101L, "DRAFT"));
        doAnswer(invocation -> {
            Map<String, Object> values = invocation.getArgument(0);
            values.put("achievementId", 501L);
            return null;
        }).when(mapper).insertHeader(anyMap());
    }

    @Test
    void createUsesGeneratedHeaderKeyBeforeDetailAndWritesEachAuditField() throws Exception {
        mvc.perform(request(post(base()), faculty).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(501))
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(2))
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.achievement.attachmentIds").isArray())
                .andExpect(jsonPath("$.meta.requestId").value("REQ-LECTURE"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(anyMap());
        order.verify(mapper).insertDetail(eq(501L), any());
        order.verify(mapper).statusHistory(501L, 101L, "REQ-LECTURE");
        verify(mapper).changeHistory(501L, "CREATE", "academicYear", null, "2025", 101L, "REQ-LECTURE");
        verify(mapper).changeHistory(501L, "CREATE", "semester", null, "2", 101L, "REQ-LECTURE");
        mvc.perform(request(get(base() + "/501"), faculty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementContent").value("강의 개선 보고서"));
    }

    @Test
    void updatePreservesEvaluationYearAndAuditsOldAndNewAcademicYearSemesterAndContent() throws Exception {
        when(mapper.find(501L, true)).thenReturn(new LectureImprovementRow(
                501L, "LI-001", 101L, "faculty", "DEPT", "2026", "LECTURE_IMPROVEMENT",
                LocalDate.parse("2026-04-10"), "DRAFT", "이전 내용", 2024, 1, "[]"));
        when(guards.countEvaluationDatePeriods("2026", 101L, LocalDate.parse("2025-12-31"))).thenReturn(0);
        mvc.perform(request(put(base() + "/501"), faculty).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(2))
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        verify(guards).countActiveInputPeriods("2026", 101L);
        verify(mapper).changeHistory(501L, "UPDATE", "academicYear", "2024", "2025", 101L, "REQ-LECTURE");
        verify(mapper).changeHistory(501L, "UPDATE", "semester", "1", "2", 101L, "REQ-LECTURE");
        verify(mapper).changeHistory(
                501L, "UPDATE", "achievementContent", "이전 내용", "강의 개선 보고서", 101L, "REQ-LECTURE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"academicYear", "semester", "achievementContent", "managementItemCode", "achievementDate"})
    void missingRequiredFieldsAreObjectErrorsBeforePersistence(String field) throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        ((com.fasterxml.jackson.databind.node.ObjectNode) json).remove(field);
        mvc.perform(request(post(base()), faculty).content(json.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields." + field).exists());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void invalidSemesterIs400ForPut() throws Exception {
        mvc.perform(request(put(base() + "/501"), faculty).content(body.replace("\"semester\":2", "\"semester\":3")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.semester").exists());
        verify(mapper, never()).updateDetail(anyLong(), any());
    }

    @Test
    void confirmedRowIs409AndOriginalIsUnchanged() throws Exception {
        LectureImprovementRow original = row(101L, "EVALUATION_CONFIRMED");
        when(mapper.find(501L, true)).thenReturn(original);
        mvc.perform(request(put(base() + "/501"), faculty).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        verify(mapper, never()).updateHeader(anyLong(), any(), anyString(), anyLong());
        verify(mapper, never()).updateDetail(anyLong(), any());
        assertThat(mapper.find(501L, true)).isEqualTo(original);
    }

    @Test
    void submittedRowIsNotEditable() throws Exception {
        when(mapper.find(501L, true)).thenReturn(row(101L, "SUBMITTED"));
        mvc.perform(request(put(base() + "/501"), faculty).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
        verify(mapper, never()).updateDetail(anyLong(), any());
    }

    @Test
    void closedPeriodLeavesOriginalUnchanged() throws Exception {
        when(guards.countActiveInputPeriods("2026", 101L)).thenReturn(0);
        mvc.perform(request(put(base() + "/501"), faculty).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        verify(mapper, never()).updateDetail(anyLong(), any());
    }

    @Test
    void ownerCheckPrecedesLockAndPeriodOnPut() throws Exception {
        when(mapper.find(501L, true)).thenReturn(row(102L, "EVALUATION_CONFIRMED"));
        mvc.perform(request(put(base() + "/501"), faculty).content(body)).andExpect(status().isForbidden());
        verify(mapper, never()).updateDetail(anyLong(), any());
        verifyNoInteractions(guards);
    }

    @Test
    void getIsForbiddenOutsideScope() throws Exception {
        when(mapper.find(501L, false)).thenReturn(row(102L, "DRAFT"));
        mvc.perform(request(get(base() + "/501"), faculty)).andExpect(status().isForbidden());
    }

    @Test
    void combinedRoleReadUsesUnionRatherThanOwnerOnly() throws Exception {
        when(mapper.find(501L, false)).thenReturn(row(102L, "DRAFT"));
        when(guards.countSharedActiveOrganization(101L, 102L)).thenReturn(1);
        mvc.perform(request(get(base() + "/501"), actor(101L, "R01", "R02")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.teacherUserId").value(102));
    }

    @Test
    void getMissingRowReturns404WithoutLeakage() throws Exception {
        mvc.perform(request(get(base() + "/999"), faculty))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"R02", "R04", "R07"})
    void businessReadRolesDoNotAcquireMutationRights(String role) throws Exception {
        mvc.perform(request(post(base()), actor(101L, role)).content(body)).andExpect(status().isForbidden());
        mvc.perform(request(put(base() + "/501"), actor(101L, role)).content(body)).andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(anyMap());
        verify(mapper, never()).updateDetail(anyLong(), any());
    }

    @Test
    void r07CannotCallListOrDetail() throws Exception {
        mvc.perform(request(get(base()), actor(101L, "R07"))).andExpect(status().isForbidden());
        mvc.perform(request(get(base() + "/501"), actor(101L, "R07"))).andExpect(status().isForbidden());
    }

    @Test
    void explicitAdministratorOverrideAllowsAllFourOperations() throws Exception {
        CurrentUser admin = actor(101L, "R09");
        when(mapper.list(any(), anyLong(), anyList())).thenReturn(List.of(row(101L, "DRAFT")));
        when(mapper.count(any(), anyLong(), anyList())).thenReturn(1L);
        mvc.perform(request(get(base()), admin)).andExpect(status().isOk());
        mvc.perform(request(get(base() + "/501"), admin)).andExpect(status().isOk());
        mvc.perform(request(post(base()), admin).content(body)).andExpect(status().isOk());
        mvc.perform(request(put(base() + "/501"), admin).content(body)).andExpect(status().isOk());
    }

    @Test
    void listAndCountReceiveExactlySameFilterAndCombinedRoles() throws Exception {
        when(mapper.list(any(), anyLong(), anyList())).thenReturn(List.of(row(101L, "DRAFT")));
        when(mapper.count(any(), anyLong(), anyList())).thenReturn(1L);
        mvc.perform(request(get(base()), actor(101L, "R01", "R04"))
                        .param("managementItemCode", "LECTURE_IMPROVEMENT").param("pageSize", "50"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.managementItems[0].dataType").value("TEXT"));
        var criteria = ArgumentCaptor.forClass(LectureImprovementSearchCriteria.class);
        verify(mapper).list(criteria.capture(), eq(101L), eq(List.of("R01", "R04")));
        verify(mapper).count(eq(criteria.getValue()), eq(101L), eq(List.of("R01", "R04")));
        assertThat(criteria.getValue().managementItemCode()).isEqualTo("LECTURE_IMPROVEMENT");
    }

    @Test
    void unknownItemIsRejectedWithoutHeader() throws Exception {
        mvc.perform(request(post(base()), faculty).content(body.replace("LECTURE_IMPROVEMENT", "UNKNOWN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.managementItemCode").exists());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void ambiguousPeriodIsNotSilentlyChosen() throws Exception {
        when(mapper.evaluationYears("DEPT")).thenReturn(List.of("2026", "2027"));
        mvc.perform(request(post(base()), faculty).content(body)).andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void forgedAttachmentIsRejected() throws Exception {
        mvc.perform(request(post(base()), faculty).content(body.replace("\"attachmentIds\":[]", "\"attachmentIds\":[\"forged\"]")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.attachmentIds").exists());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void invalidPaginationIsRejected() throws Exception {
        mvc.perform(request(get(base()), faculty).param("pageSize", "30")).andExpect(status().isBadRequest());
        verify(mapper, never()).list(any(), anyLong(), anyList());
    }

    @Test
    void anonymousRequestIs401() throws Exception {
        mvc.perform(get(base())).andExpect(status().isUnauthorized());
    }

    private static String base() { return "/api/business/lecture-improvements"; }
    private static CurrentUser actor(Long id, String... roles) {
        return new CurrentUser(id, "faculty", "E101", "교원", List.of(roles), List.of());
    }
    private LectureImprovementRow row(Long owner, String state) {
        return new LectureImprovementRow(501L, "LI-001", owner, "faculty", "DEPT", "2026",
                "LECTURE_IMPROVEMENT", LocalDate.parse("2025-12-31"), state, "강의 개선 보고서", 2025, 2, "[]");
    }
    private MockHttpServletRequestBuilder request(MockHttpServletRequestBuilder builder, CurrentUser actor) {
        return builder.requestAttr("currentUser", actor).header("X-Request-Id", "REQ-LECTURE")
                .contentType(MediaType.APPLICATION_JSON);
    }
}
