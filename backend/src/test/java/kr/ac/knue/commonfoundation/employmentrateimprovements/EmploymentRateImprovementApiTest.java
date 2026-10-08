package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
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
import org.springframework.test.web.servlet.MockMvc;

/** Real controller/service/policy with a stateful mapper boundary; SQL durability is runner-owned. */
@WebMvcTest(EmploymentRateImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EmploymentRateImprovementService.class, EducationAchievementAccessPolicy.class})
class EmploymentRateImprovementApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean EmploymentRateImprovementMapper mapper;
    @MockBean EducationAchievementGuardMapper guards;
    @MockBean EffectivePermissionService menus;
    @MockBean FunctionPermissionService functions;
    private final CurrentUser teacher = user(101L, "R01");
    private EmploymentRateImprovementRow stored;
    private EmploymentRateImprovementWrite pending;
    private final List<String> histories = new ArrayList<>();
    private static final String BODY = """
            {"managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT","evaluationYear":"2026",
             "achievementDate":"2026-04-11","specialLectureStartDate":"2026-04-10",
             "specialLectureEndDate":"2026-04-12","mockExamQuestionPeriod":"4월 출제",
             "attachmentIds":["opaque-attachment"]}
            """;

    @BeforeEach
    void setup() {
        stored = null;
        histories.clear();
        when(menus.canAccess(anyLong(), anyList(), anyString())).thenReturn(true);
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guards.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.countManagementItem(anyString(), anyString())).thenReturn(1);
        when(mapper.find(anyLong())).thenAnswer(call -> stored);
        when(mapper.lock(anyLong())).thenAnswer(call -> stored);
        when(mapper.insertHeader(any())).thenAnswer(call -> {
            pending = call.getArgument(0);
            pending.setAchievementId(81L);
            return 1;
        });
        when(mapper.insertDetail(any())).thenAnswer(call -> {
            assertThat(pending.getAchievementId()).isEqualTo(81L);
            stored = materialize(pending, "DRAFT");
            return 1;
        });
        when(mapper.updateHeader(any())).thenAnswer(call -> {
            pending = call.getArgument(0);
            return 1;
        });
        when(mapper.updateDetail(any())).thenAnswer(call -> {
            stored = materialize(pending, stored.achievementStatus());
            return 1;
        });
        doAnswer(call -> {
            histories.add(call.getArgument(2) + ":" + call.getArgument(3) + "->" + call.getArgument(4));
            return null;
        }).when(mapper).insertChangeHistory(anyLong(), anyString(), anyString(), nullable(String.class),
                anyString(), anyLong(), anyString());
    }

    @Test
    void createPersistsHeaderThenDetailAndHistoriesAndGetReadsItBack() throws Exception {
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .header("X-Request-Id", "create-trace").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievement.achievementId").value(81))
                .andExpect(jsonPath("$.meta.requestId").value("create-trace"));
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.specialLectureEndDate").value("2026-04-12"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(any());
        order.verify(mapper).insertDetail(any());
        order.verify(mapper).find(81L);
        assertThat(histories).anyMatch(value -> value.startsWith("mockExamQuestionPeriod:null->"));
        verify(mapper).insertStatusHistory(81L, 101L, "create-trace");
    }

    @Test
    void updatePreservesEvaluationYearAndRecordsAllChangedValues() throws Exception {
        stored = seed("DRAFT", 101L);
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).header("X-Request-Id", "update-trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("2026-04-11", "2025-12-31").replace("\"2026\"", "\"2025\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"));
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementDate").value("2025-12-31"));
        assertThat(pending.getEvaluationYear()).isEqualTo("2026");
        assertThat(histories).anyMatch(value -> value.contains("achievementDate:\"2026-04-10\"->\"2025-12-31\""));
        assertThat(histories).anyMatch(value -> value.startsWith("specialLectureEndDate:"));
        assertThat(histories).anyMatch(value -> value.startsWith("attachmentIds:"));
    }

    @Test
    void listReturnsScopedFilteredCountAndPage() throws Exception {
        stored = seed("DRAFT", 102L);
        var multiRole = user(101L, "R01", "R02");
        when(mapper.list(any(), eq(101L), eq(multiRole.roles()))).thenReturn(List.of(stored));
        when(mapper.count(any(), eq(101L), eq(multiRole.roles()))).thenReturn(1L);
        mvc.perform(get("/api/business/employment-rate-improvements").requestAttr("currentUser", multiRole)
                        .param("managementItemCode", "EMPLOYMENT_RATE_IMPROVEMENT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].teacherUserId").value(102));
    }

    @Test
    void detailUnionsSelfAndDepartmentScope() throws Exception {
        stored = seed("DRAFT", 102L);
        when(guards.countSharedActiveOrganization(101L, 102L)).thenReturn(1);
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", user(101L, "R01", "R02")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.teacherUserId").value(102));
    }

    @Test
    void createRejectsR07WithoutWrites() throws Exception {
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", user(107L, "R07"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void listRejectsR07() throws Exception {
        mvc.perform(get("/api/business/employment-rate-improvements").requestAttr("currentUser", user(107L, "R07")))
                .andExpect(status().isForbidden());
        verify(mapper, never()).list(any(), any(), any());
    }

    @Test
    void detailRejectsOutOfScope() throws Exception {
        stored = seed("DRAFT", 102L);
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRejectsOtherOwner() throws Exception {
        stored = seed("DRAFT", 102L);
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void createMissingManagementItemHasFieldError() throws Exception {
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"achievementDate\":\"2026-04-11\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void updateMissingManagementItemHasFieldError() throws Exception {
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2026-04-11\"}"))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void createRejectsReversedSpecialLectureDates() throws Exception {
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("2026-04-12", "2026-04-09")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("specialLectureEndDate"));
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void createRejectsInactiveManagementItem() throws Exception {
        when(mapper.countManagementItem(anyString(), anyString())).thenReturn(0);
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void createRejectsDuplicateInsteadOfUpsert() throws Exception {
        when(mapper.countDuplicate(any())).thenReturn(1);
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void createRejectsInactivePeriod() throws Exception {
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.message").value(
                        "PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다."));
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void updateInactivePeriodPreservesOriginal() throws Exception {
        stored = seed("DRAFT", 101L);
        var original = stored;
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
        assertThat(stored).isSameAs(original);
        assertThat(histories).isEmpty();
    }

    @Test
    void createConfirmedTargetHasNoWrites() throws Exception {
        when(guards.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void updateConfirmedRecordPreservesOriginal() throws Exception {
        stored = seed("EVALUATION_CONFIRMED", 101L);
        var original = stored;
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
        assertThat(stored).isSameAs(original);
        assertThat(histories).isEmpty();
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void updateSubmittedRecordIsNotEditable() throws Exception {
        stored = seed("SUBMITTED", 101L);
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void outsideEvaluationDateSavesWithWarning() throws Exception {
        when(guards.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(0);
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.occurredDateWarning").value(true));
    }

    @Test
    void detailNotFound() throws Exception {
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 404)
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void updateNotFound() throws Exception {
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 404)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void administratorOverrideCreatesWithoutBusinessRole() throws Exception {
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", user(1L, "R09"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    void functionDenialBlocksWrites() throws Exception {
        when(functions.evaluate(any())).thenThrow(new ForbiddenException());
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void unauthenticatedListIs401() throws Exception {
        mvc.perform(get("/api/business/employment-rate-improvements")).andExpect(status().isUnauthorized());
    }

    @Test
    void unexpectedSqlErrorDoesNotLeakDetails() throws Exception {
        when(mapper.find(anyLong())).thenThrow(new RuntimeException("secret SQL credentials"));
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret SQL"))));
    }

    private static CurrentUser user(Long id, String... roles) {
        return new CurrentUser(id, "faculty", "E101", "교원", List.of(roles), List.of());
    }

    private EmploymentRateImprovementRow materialize(EmploymentRateImprovementWrite write, String status) {
        var r = write.getRequest();
        return new EmploymentRateImprovementRow(write.getAchievementId(), write.getTeacherUserId(), "DEPT", "교원",
                write.getEvaluationYear(), r.managementItemCode(), r.achievementDate(), r.achievementName(),
                write.getDetail(), r.attachmentRef(), write.getAttachments(), status,
                r.specialLectureStartDate(), r.specialLectureEndDate(), r.mockExamQuestionPeriod(),
                LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private EmploymentRateImprovementRow seed(String status, Long teacherId) {
        return new EmploymentRateImprovementRow(81L, teacherId, "DEPT", "교원", "2026",
                "EMPLOYMENT_RATE_IMPROVEMENT", LocalDate.parse("2026-04-10"), null, "{}", null, "[]", status,
                LocalDate.parse("2026-04-10"), LocalDate.parse("2026-04-10"), "기존 출제",
                LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}
