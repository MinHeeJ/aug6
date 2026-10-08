package kr.ac.knue.commonfoundation.lectureimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.common.educationachievements.EducationAchievementAccessPolicy;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP tests keep controller, service and access policy real; only persistence/permission adapters are doubled. */
@WebMvcTest(LectureImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, LectureImprovementService.class, EducationAchievementAccessPolicy.class})
class LectureImprovementApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean LectureImprovementMapper mapper;
    @MockBean EducationAchievementGuardMapper guards;
    @MockBean EffectivePermissionService menus;
    @MockBean FunctionPermissionService functions;
    final CurrentUser teacher = user("R01");
    LectureImprovementRow stored;
    Map<String, Object> pending;

    @BeforeEach
    void setup() {
        stored = row(101L, "DRAFT");
        when(menus.canAccess(anyLong(), anyList(), anyString())).thenReturn(true);
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guards.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.organization(anyLong())).thenReturn("KNUE-DEPT-COMP");
        when(mapper.validManagementItem(anyString(), anyString(), anyString())).thenReturn(1);
        when(mapper.validSemester(anyString())).thenReturn(1);
        when(mapper.find(82L)).thenAnswer(call -> stored);
        when(mapper.lock(82L)).thenAnswer(call -> stored);
        doAnswer(call -> {
            pending = new HashMap<>(call.getArgument(0));
            Map<String, Object> command = call.getArgument(0);
            command.put("id", 82L);
            return 1;
        }).when(mapper).insertHeader(anyMap());
        doAnswer(call -> {
            pending = new HashMap<>(call.getArgument(0));
            return 1;
        }).when(mapper).updateHeader(anyMap());
        doAnswer(call -> materialize(call.getArgument(1))).when(mapper).insertDetail(anyLong(), any());
        doAnswer(call -> materialize(call.getArgument(1))).when(mapper).updateDetail(anyLong(), any());
    }

    private int materialize(LectureImprovementRequest body) {
        stored = new LectureImprovementRow(82L, (Long) pending.get("teacherUserId"),
                (String) pending.get("organizationCode"), (String) pending.get("evaluationYear"),
                (String) pending.get("managementItemCode"), body.achievementDate(), body.achievementContent(),
                body.academicYear(), body.semester(), "DRAFT", body.attachmentIds() == null ? List.of() : body.attachmentIds());
        return 1;
    }

    @Test
    void createUsesGeneratedKeyThenDetailAndReadbackWithHistory() throws Exception {
        mvc.perform(post("/api/business/lecture-improvements").requestAttr("currentUser", teacher)
                .header("X-Request-Id", "lecture-create").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.academicYear").value(2026))
                .andExpect(jsonPath("$.data.achievement.semester").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("lecture-create"));
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", teacher)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementContent").value("강의 개선 내용"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(anyMap());
        order.verify(mapper).insertDetail(eq(82L), any());
        order.verify(mapper).insertStatusHistory(82L, 101L, "lecture-create");
        verify(mapper).insertChangeHistory(82L, "CREATE", "semester", null, "2", 101L, "lecture-create");
    }

    @Test
    void updateRetainsEvaluationYearAndAuditsChangedValues() throws Exception {
        mvc.perform(put("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", teacher).header("X-Request-Id", "lecture-update")
                .contentType(MediaType.APPLICATION_JSON).content(body().replace("2026", "2027")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2027));
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", teacher)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.semester").value(2));
        verify(mapper).insertChangeHistory(82L, "UPDATE", "academicYear", "2026", "2027", 101L, "lecture-update");
        verify(mapper).insertChangeHistory(82L, "UPDATE", "semester", "1", "2", 101L, "lecture-update");
        verify(guards).countActiveInputPeriods("2026", 101L);
    }

    @Test
    void listUsesSameFiltersForRowsAndTotalWithMultiRoleScope() throws Exception {
        CurrentUser multi = user("R01", "R02", "R04");
        when(mapper.list(any(), eq(101L), eq(multi.roles()))).thenReturn(List.of(stored));
        when(mapper.count(any(), eq(101L), eq(multi.roles()))).thenReturn(1L);
        mvc.perform(get("/api/business/lecture-improvements").requestAttr("currentUser", multi)
                .param("managementItemCode", " LECTURE_IMPROVEMENT ").param("achievementStatus", "DRAFT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].achievementId").value(82));
        var capture = org.mockito.ArgumentCaptor.forClass(LectureImprovementSearchCriteria.class);
        verify(mapper).list(capture.capture(), eq(101L), eq(multi.roles()));
        verify(mapper).count(eq(capture.getValue()), eq(101L), eq(multi.roles()));
        assertThat(capture.getValue().managementItemCode()).isEqualTo("LECTURE_IMPROVEMENT");
    }

    @Test
    void detailUsesUnionRatherThanSelfOnlyForMultiRoleReader() throws Exception {
        stored = row(102L, "DRAFT");
        when(guards.countSharedActiveOrganization(101L, 102L)).thenReturn(1);
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", user("R01", "R02"))).andExpect(status().isOk());
    }

    @Test
    void detailNotFound() throws Exception {
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 999L)
                .requestAttr("currentUser", teacher)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void updateNotFound() throws Exception {
        mvc.perform(put("/api/business/lecture-improvements/{achievementId}", 999L)
                .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isNotFound());
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void listRejectsUnadmittedRole() throws Exception {
        mvc.perform(get("/api/business/lecture-improvements").requestAttr("currentUser", user("R07")))
                .andExpect(status().isForbidden());
        verify(mapper, never()).list(any(), any(), any());
    }

    @Test
    void detailRejectsUnadmittedRole() throws Exception {
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", user("R07"))).andExpect(status().isForbidden());
    }

    @Test
    void createRejectsReadOnlyRole() throws Exception {
        mvc.perform(post("/api/business/lecture-improvements").requestAttr("currentUser", user("R02"))
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void updateRejectsReadOnlyRole() throws Exception {
        mvc.perform(put("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", user("R04")).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void updateRejectsOtherOwnerWithoutWrites() throws Exception {
        stored = row(102L, "DRAFT");
        mvc.perform(put("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(anyMap());
        assertThat(stored.teacherUserId()).isEqualTo(102L);
    }

    @Test
    void detailRejectsOutsideScope() throws Exception {
        stored = row(102L, "DRAFT");
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", teacher)).andExpect(status().isForbidden());
    }

    @Test
    void createRequiresAcademicYear() throws Exception {
        mvc.perform(post("/api/business/lecture-improvements").requestAttr("currentUser", teacher)
                .contentType(MediaType.APPLICATION_JSON).content(body().replace("\"academicYear\":2026,", "")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void createRequiresSemester() throws Exception {
        mvc.perform(post("/api/business/lecture-improvements").requestAttr("currentUser", teacher)
                .contentType(MediaType.APPLICATION_JSON).content(body().replace("\"semester\":2", "\"semester\":null")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRejectsInvalidSemester() throws Exception {
        mvc.perform(put("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("\"semester\":2", "\"semester\":3"))).andExpect(status().isBadRequest());
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void inactiveSemesterIsValidatedAgainstDatabase() throws Exception {
        when(mapper.validSemester("2")).thenReturn(0);
        mvc.perform(post("/api/business/lecture-improvements").requestAttr("currentUser", teacher)
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void inactiveManagementItemCannotBeSaved() throws Exception {
        when(mapper.validManagementItem(any(), any(), any())).thenReturn(0);
        mvc.perform(post("/api/business/lecture-improvements").requestAttr("currentUser", teacher)
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void confirmedRowRemainsUnchanged() throws Exception {
        stored = row(101L, "EVALUATION_CONFIRMED");
        LectureImprovementRow before = stored;
        mvc.perform(put("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.message").value(
                        org.hamcrest.Matchers.containsString("CONFIRMED_DATA_LOCKED")));
        assertThat(stored).isEqualTo(before);
        verify(mapper, never()).updateHeader(anyMap());
        verify(mapper, never()).updateDetail(anyLong(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void submittedRowCannotBeEdited() throws Exception {
        stored = row(101L, "SUBMITTED");
        mvc.perform(put("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isConflict());
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void targetFinalizationPrecedesFunctionEvaluator() throws Exception {
        when(guards.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        mvc.perform(put("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isConflict());
        verify(functions, never()).evaluate(any());
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void createOutsideInputPeriodHasNoSideEffects() throws Exception {
        when(guards.countActiveInputPeriods(any(), any())).thenReturn(0);
        mvc.perform(post("/api/business/lecture-improvements").requestAttr("currentUser", teacher)
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isConflict());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void updateOutsideInputPeriodHasNoSideEffects() throws Exception {
        when(guards.countActiveInputPeriods(any(), any())).thenReturn(0);
        mvc.perform(put("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isConflict());
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void occurredDateWarningStillCreatesWithIndependentEvaluationYear() throws Exception {
        when(guards.countEvaluationDatePeriods(any(), any(), any())).thenReturn(0);
        mvc.perform(post("/api/business/lecture-improvements").requestAttr("currentUser", teacher)
                .contentType(MediaType.APPLICATION_JSON).content(body().replace("2026-04-11", "2025-12-31")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"));
    }

    @Test
    void menuDeniedPreventsListQuery() throws Exception {
        when(menus.canAccess(anyLong(), anyList(), anyString())).thenReturn(false);
        mvc.perform(get("/api/business/lecture-improvements").requestAttr("currentUser", teacher))
                .andExpect(status().isForbidden());
        verify(mapper, never()).list(any(), any(), any());
    }

    @Test
    void functionDeniedPreventsMutation() throws Exception {
        when(functions.evaluate(any())).thenThrow(new ForbiddenException());
        mvc.perform(post("/api/business/lecture-improvements").requestAttr("currentUser", teacher)
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void adminOverrideDoesNotBypassFinalization() throws Exception {
        when(guards.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        mvc.perform(put("/api/business/lecture-improvements/{achievementId}", 82L)
                .requestAttr("currentUser", user("R09")).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isConflict());
    }

    @Test
    void administratorCanCreate() throws Exception {
        mvc.perform(post("/api/business/lecture-improvements").requestAttr("currentUser", user("R09"))
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isOk());
    }

    @Test
    void unexpectedPersistenceErrorIsSanitized() throws Exception {
        when(mapper.insertHeader(anyMap())).thenThrow(new IllegalStateException("private-db-secret"));
        mvc.perform(post("/api/business/lecture-improvements").requestAttr("currentUser", teacher)
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-db-secret"))));
    }

    @Test
    void missingSessionPrincipalIsUnauthorized() throws Exception {
        mvc.perform(get("/api/business/lecture-improvements")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 82L)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/business/lecture-improvements").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/business/lecture-improvements/{achievementId}", 82L)
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isUnauthorized());
    }

    @Test
    void durableContractFixtureIsAvailable() throws Exception {
        try (var stream = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThat(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .contains("operationId: createLectureImprovement", "operationId: updateLectureImprovement");
        }
    }

    private static CurrentUser user(String... roles) {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of(roles), List.of());
    }

    private static LectureImprovementRow row(Long owner, String status) {
        return new LectureImprovementRow(82L, owner, "KNUE-DEPT-COMP", "2026", "LECTURE_IMPROVEMENT",
                LocalDate.parse("2026-04-10"), "기존 내용", 2026, 1, status, List.of());
    }

    private String body() {
        return """
                {"managementItemCode":"LECTURE_IMPROVEMENT","achievementDate":"2026-04-11",
                 "achievementContent":"강의 개선 내용","academicYear":2026,"semester":2,"attachmentIds":[]}
                """;
    }
}
