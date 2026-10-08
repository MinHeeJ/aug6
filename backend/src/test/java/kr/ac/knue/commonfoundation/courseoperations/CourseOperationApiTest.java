package kr.ac.knue.commonfoundation.courseoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Real HTTP/controller/service/policy; the stateful mapper boundary does not prove SQL durability. */
@WebMvcTest(CourseOperationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, CourseOperationService.class, EducationAchievementAccessPolicy.class})
class CourseOperationApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean CourseOperationMapper mapper;
    @MockBean EducationAchievementGuardMapper guard;
    @MockBean EffectivePermissionService menus;
    @MockBean FunctionPermissionService functions;
    private final CurrentUser teacher = user(101L, "R01");
    private final CurrentUser operator = user(107L, "R07");
    private final Map<Long, CourseOperationRow> rows = new LinkedHashMap<>();
    private final Map<Long, Map<String, Object>> headers = new LinkedHashMap<>();
    private final List<String> histories = new ArrayList<>();

    @BeforeEach
    void setup() {
        rows.clear();
        headers.clear();
        histories.clear();
        when(menus.canAccess(any(), anyList(), anyString())).thenReturn(true);
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.organization(anyLong())).thenReturn("KNUE-DEPT-COMP");
        when(mapper.activeYear(anyLong())).thenReturn("2026");
        when(mapper.editableItem(anyString(), anyString(), anyString())).thenReturn(1);
        when(mapper.find(anyLong())).thenAnswer(call -> rows.get(call.getArgument(0)));
        when(mapper.lock(anyLong())).thenAnswer(call -> rows.get(call.getArgument(0)));
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            long id = 1000L + headers.size();
            values.put("id", id);
            headers.put(id, new LinkedHashMap<>(values));
            return null;
        }).when(mapper).insertHeader(anyMap());
        doAnswer(call -> {
            Long id = call.getArgument(0);
            rows.put(id, rowFrom(headers.get(id), call.getArgument(1), "DRAFT"));
            return null;
        }).when(mapper).insertDetail(anyLong(), anyString());
        when(mapper.updateHeader(anyMap())).thenAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            headers.put((Long) values.get("id"), new LinkedHashMap<>(values));
            return 1;
        });
        doAnswer(call -> {
            Long id = call.getArgument(0);
            rows.put(id, rowFrom(headers.get(id), call.getArgument(1), rows.get(id).achievementStatus()));
            return null;
        }).when(mapper).updateDetail(anyLong(), anyString());
        doAnswer(call -> {
            histories.add(call.getArgument(2) + " -> " + call.getArgument(3));
            return null;
        }).when(mapper).changeHistory(
                anyLong(), anyString(), nullable(String.class), anyString(), anyLong(), anyString());
        when(mapper.list(any(), anyLong(), anyList())).thenAnswer(call -> scoped(
                call.getArgument(0), call.getArgument(1), call.getArgument(2)));
        when(mapper.count(any(), anyLong(), anyList())).thenAnswer(call -> (long) scoped(
                call.getArgument(0), call.getArgument(1), call.getArgument(2)).size());
    }

    @Test
    void createReadAndListPreserveDetailsAndHistory() throws Exception {
        mvc.perform(write(post("/api/business/course-operations"), teacher, body("개설 실적")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.performanceDetails").value("개설 실적"))
                .andExpect(jsonPath("$.meta.requestId").value("course-request"));
        mvc.perform(get("/api/business/course-operations/{achievementId}", 1000L).requestAttr("currentUser", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceDetails").value("개설 실적"));
        mvc.perform(get("/api/business/course-operations").requestAttr("currentUser", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].performanceDetails").value("개설 실적"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        assertThat(histories).singleElement().asString().contains("개설 실적", "attachment-token");
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(anyMap());
        order.verify(mapper).insertDetail(eq(1000L), eq("개설 실적"));
        order.verify(mapper).statusHistory(1000L, 101L, "course-request");
    }

    @Test
    void updatePreservesYearAndAuditsEveryChangedValueAndWarns() throws Exception {
        rows.put(12L, row(12L, 101L, "DRAFT"));
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(0);
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 12L), teacher,
                new CourseOperationRequest("COURSE_OPERATION", LocalDate.parse("2025-12-31"), "수정 실적",
                        List.of("new-attachment"), "2025")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        mvc.perform(get("/api/business/course-operations/{achievementId}", 12L).requestAttr("currentUser", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceDetails").value("수정 실적"))
                .andExpect(jsonPath("$.data.attachmentIds[0]").value("new-attachment"));
        assertThat(histories).singleElement().asString()
                .contains("원본", "수정 실적", "2026", "2025-12-31", "new-attachment");
        verify(guard).countActiveInputPeriods("2026", 101L);
    }

    @Test
    void listFilteredDepartmentUnionMatchesTotal() throws Exception {
        rows.put(12L, row(12L, 101L, "DRAFT"));
        rows.put(13L, row(13L, 102L, "DRAFT"));
        rows.put(14L, row(14L, 103L, "SUBMITTED"));
        when(guard.countSharedActiveOrganization(101L, 102L)).thenReturn(1);
        CurrentUser multiRole = user(101L, "R01", "R02");
        mvc.perform(get("/api/business/course-operations").requestAttr("currentUser", multiRole)
                        .param("achievementStatus", "DRAFT").param("managementItemCode", "COURSE_OPERATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mvc.perform(get("/api/business/course-operations/{achievementId}", 13L).requestAttr("currentUser", multiRole))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.teacherUserId").value(102));
    }

    @Test
    void detailOutOfScopeIsForbidden() throws Exception {
        rows.put(12L, row(12L, 102L, "DRAFT"));
        mvc.perform(get("/api/business/course-operations/{achievementId}", 12L).requestAttr("currentUser", teacher))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void updateOtherOwnerIsForbiddenWithoutWrites() throws Exception {
        rows.put(12L, row(12L, 102L, "DRAFT"));
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 12L), teacher, body("수정")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        unchanged(12L);
    }

    @Test
    void updateConfirmedRowIsConflictAndUnchanged() throws Exception {
        rows.put(12L, row(12L, 101L, "EVALUATION_CONFIRMED"));
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 12L), teacher, body("수정")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value(
                        org.hamcrest.Matchers.containsString("CONFIRMED_DATA_LOCKED")));
        unchanged(12L);
    }

    @Test
    void updateSubmittedRowIsConflictAndUnchanged() throws Exception {
        rows.put(12L, row(12L, 101L, "SUBMITTED"));
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 12L), teacher, body("수정")))
                .andExpect(status().isConflict());
        unchanged(12L);
    }

    @Test
    void createFinalizedTeacherIsConflictWithoutInsert() throws Exception {
        when(guard.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        mvc.perform(write(post("/api/business/course-operations"), teacher, body("신규")))
                .andExpect(status().isConflict());
        verify(mapper, never()).insertHeader(anyMap());
        assertThat(histories).isEmpty();
    }

    @Test
    void updateFinalizedTeacherIsConflictAndUnchanged() throws Exception {
        rows.put(12L, row(12L, 101L, "DRAFT"));
        when(guard.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 12L), teacher, body("수정")))
                .andExpect(status().isConflict());
        unchanged(12L);
    }

    @Test
    void createInactivePeriodIsConflict() throws Exception {
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(write(post("/api/business/course-operations"), teacher, body("신규")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value(
                        org.hamcrest.Matchers.containsString("PERIOD_NOT_ACTIVE")));
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void updateInactivePeriodIsConflictAndUnchanged() throws Exception {
        rows.put(12L, row(12L, 101L, "DRAFT"));
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 12L), teacher, body("수정")))
                .andExpect(status().isConflict());
        unchanged(12L);
    }

    @Test
    void createMissingPerformanceDetailsIsValidationError() throws Exception {
        mvc.perform(write(post("/api/business/course-operations"), teacher, body(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'performanceDetails')]").isNotEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void updateMissingPerformanceDetailsIsValidationError() throws Exception {
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 12L), teacher, body(" ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'performanceDetails')]").isNotEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void createInvalidManagementItemIsValidationError() throws Exception {
        when(mapper.editableItem(anyString(), anyString(), anyString())).thenReturn(0);
        mvc.perform(write(post("/api/business/course-operations"), teacher, body("신규")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode"));
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void createDuplicateIsConflict() throws Exception {
        when(mapper.duplicate(anyLong(), anyString(), any(), isNull())).thenReturn(1);
        mvc.perform(write(post("/api/business/course-operations"), teacher, body("신규")))
                .andExpect(status().isConflict());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void updateDuplicateIsConflictAndUnchanged() throws Exception {
        rows.put(12L, row(12L, 101L, "DRAFT"));
        when(mapper.duplicate(anyLong(), anyString(), any(), eq(12L))).thenReturn(1);
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 12L), teacher, body("수정")))
                .andExpect(status().isConflict());
        unchanged(12L);
    }

    @Test
    void getMissingIsNotFound() throws Exception {
        mvc.perform(get("/api/business/course-operations/{achievementId}", 999L).requestAttr("currentUser", teacher))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void updateMissingIsNotFound() throws Exception {
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 999L), teacher, body("수정")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void listForbiddenRole() throws Exception {
        mvc.perform(get("/api/business/course-operations").requestAttr("currentUser", operator))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(mapper);
    }

    @Test
    void getForbiddenRole() throws Exception {
        mvc.perform(get("/api/business/course-operations/{achievementId}", 12L).requestAttr("currentUser", operator))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(mapper);
    }

    @Test
    void createForbiddenRole() throws Exception {
        mvc.perform(write(post("/api/business/course-operations"), operator, body("신규")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(mapper);
    }

    @Test
    void updateForbiddenRole() throws Exception {
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 12L), operator, body("수정")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(mapper);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/business/course-operations"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/business/course-operations/{achievementId}", 12L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/business/course-operations").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body("신규"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateRequiresAuthentication() throws Exception {
        mvc.perform(put("/api/business/course-operations/{achievementId}", 12L).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body("수정"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void menuDenialPreventsCreate() throws Exception {
        when(menus.canAccess(any(), anyList(), anyString())).thenReturn(false);
        mvc.perform(write(post("/api/business/course-operations"), teacher, body("신규")))
                .andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void functionDenialPreventsCreate() throws Exception {
        when(functions.evaluate(any())).thenThrow(new ForbiddenException());
        mvc.perform(write(post("/api/business/course-operations"), teacher, body("신규")))
                .andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void administratorBypassesRolesAndScopeButCannotMutateConfirmedRow() throws Exception {
        rows.put(12L, row(12L, 102L, "DRAFT"));
        CurrentUser admin = user(1L, "R09");
        mvc.perform(get("/api/business/course-operations/{achievementId}", 12L).requestAttr("currentUser", admin))
                .andExpect(status().isOk());
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 12L), admin, body("관리자 수정")))
                .andExpect(status().isOk());
        rows.put(12L, row(12L, 102L, "EVALUATION_CONFIRMED"));
        mvc.perform(write(put("/api/business/course-operations/{achievementId}", 12L), admin, body("관리자 수정")))
                .andExpect(status().isConflict());
    }

    @Test
    void unexpectedFailureNeverLeaksSqlOrSecret() throws Exception {
        when(mapper.find(12L)).thenThrow(new IllegalStateException("SELECT secret FROM internal_table"));
        mvc.perform(get("/api/business/course-operations/{achievementId}", 12L).requestAttr("currentUser", teacher))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }

    private void unchanged(Long id) {
        assertThat(rows.get(id).performanceDetails()).isEqualTo("원본");
        assertThat(histories).isEmpty();
        verify(mapper, never()).updateHeader(anyMap());
        verify(mapper, never()).updateDetail(anyLong(), anyString());
    }

    private MockHttpServletRequestBuilder write(
            MockHttpServletRequestBuilder request, CurrentUser user, CourseOperationRequest body) throws Exception {
        return request.requestAttr("currentUser", user).header("X-Request-Id", "course-request")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    private CourseOperationRequest body(String details) {
        return new CourseOperationRequest("COURSE_OPERATION", LocalDate.parse("2026-04-11"),
                details, List.of("attachment-token"), "2026");
    }

    private CurrentUser user(Long id, String... roles) {
        return new CurrentUser(id, "faculty", "E0101", "교원", List.of(roles), List.of());
    }

    private CourseOperationRow row(Long id, Long owner, String status) {
        return new CourseOperationRow(id, owner, "교원", "KNUE-DEPT-COMP", "2026", "COURSE_OPERATION",
                LocalDate.parse("2026-04-10"), "원본", status, List.of(),
                LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private CourseOperationRow rowFrom(Map<String, Object> values, String details, String status) throws Exception {
        CourseOperationRequest body = (CourseOperationRequest) values.get("body");
        return new CourseOperationRow((Long) values.get("id"), (Long) values.get("owner"), "교원",
                (String) values.get("organization"), (String) values.get("year"), body.managementItemCode(),
                body.achievementDate(), details, status, body.attachmentIds(),
                LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private List<CourseOperationRow> scoped(CourseOperationSearchCriteria criteria, Long id, List<String> roles) {
        return rows.values().stream().filter(row -> roles.contains("R09")
                        || (roles.contains("R01") && row.teacherUserId().equals(id))
                        || (roles.contains("R02") && guard.countSharedActiveOrganization(id, row.teacherUserId()) > 0)
                        || (roles.contains("R04") && guard.countCertificationScope(id, row.teacherUserId()) > 0))
                .filter(row -> criteria.managementItemCode() == null
                        || criteria.managementItemCode().equals(row.managementItemCode()))
                .filter(row -> criteria.achievementStatus() == null
                        || criteria.achievementStatus().equals(row.achievementStatus())).toList();
    }
}
