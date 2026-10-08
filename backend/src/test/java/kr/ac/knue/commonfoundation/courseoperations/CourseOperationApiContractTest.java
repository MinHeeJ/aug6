package kr.ac.knue.commonfoundation.courseoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
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

/** HTTP requests exercise the actual service and shared guard; only SQL adapters are mocked. */
@WebMvcTest(CourseOperationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CourseOperationExceptionAdvice.class, CourseOperationService.class, EducationAchievementGuardService.class})
class CourseOperationApiContractTest {
    @Autowired MockMvc mvc;
    @MockBean CourseOperationMapper mapper;
    @MockBean EducationAchievementGuardMapper scopes;

    private final CurrentUser faculty = user(101L, "R01");
    private CourseOperationRow persisted;
    private static final String BODY = """
            {"managementItemCode":"COURSE_OPERATION","achievementDate":"2026-04-11",
             "performanceDetails":"새 운영내역","attachmentIds":["opaque-id"]}
            """;

    @BeforeEach
    void setup() {
        persisted = row(101L, "DRAFT", "이전 운영내역", LocalDate.parse("2026-04-10"));
        when(scopes.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(scopes.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.allowedItem(anyLong(), anyString(), anyString())).thenReturn(1);
        when(mapper.organization(anyLong())).thenReturn("KNUE-DEPT-COMP");
        when(mapper.find(eq(55L), anyBoolean())).thenAnswer(call -> persisted);
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            values.put("id", 55L);
            return null;
        }).when(mapper).insertHeader(anyMap());
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            persisted = row(101L, "DRAFT", (String) values.get("performance"), (LocalDate) values.get("date"));
            return null;
        }).when(mapper).insertDetail(anyMap());
        when(mapper.updateHeader(anyMap())).thenReturn(1);
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            persisted = row(persisted.teacherUserId(), persisted.achievementStatus(),
                    (String) values.get("performance"), (LocalDate) values.get("date"));
            return null;
        }).when(mapper).updateDetail(anyMap());
        when(mapper.list(any(), anyLong(), anyList())).thenAnswer(call -> List.of(persisted));
        when(mapper.count(any(), anyLong(), anyList())).thenReturn(1L);
    }

    @Test
    void classpathContractContainsOwnedOperations() throws Exception {
        String contract = new String(new ClassPathResource("contracts/openapi.yaml")
                .getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(contract).contains("operationId: listCourseOperations", "operationId: getCourseOperation",
                "operationId: createCourseOperation", "operationId: updateCourseOperation");
    }

    @Test
    void createThenListAndDetailMatchWithGeneratedKeyAndHistory() throws Exception {
        perform(post("/api/business/course-operations").content(BODY), faculty)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.performanceDetails").value("새 운영내역"))
                .andExpect(jsonPath("$.meta.requestId").value("course-test"));
        perform(get("/api/business/course-operations"), faculty)
                .andExpect(jsonPath("$.data.achievements[0].performanceDetails").value("새 운영내역"));
        perform(get("/api/business/course-operations/55"), faculty)
                .andExpect(jsonPath("$.data.performanceDetails").value("새 운영내역"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(anyMap());
        order.verify(mapper).insertDetail(argThat(v -> Long.valueOf(55).equals(v.get("id"))));
        order.verify(mapper).insertStatus(anyMap());
        order.verify(mapper).find(55L, false);
        verify(mapper).insertHistory(eq(55L), eq("CREATE"), isNull(), contains("새 운영내역"),
                eq(101L), eq("course-test"));
    }

    @Test
    void updatePreservesYearAndAuditsAllChangedFields() throws Exception {
        String body = BODY.replace("2026-04-11", "2025-12-31");
        when(scopes.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(0);
        perform(put("/api/business/course-operations/55").content(body), faculty)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.achievement.achievementDate").value("2025-12-31"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        perform(get("/api/business/course-operations/55"), faculty)
                .andExpect(jsonPath("$.data.performanceDetails").value("새 운영내역"));
        verify(scopes).countActiveInputPeriods("2026", 101L);
        verify(mapper).updateHeader(argThat(v -> "2026".equals(v.get("year"))));
        ArgumentCaptor<String> old = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> updated = ArgumentCaptor.forClass(String.class);
        verify(mapper).insertHistory(eq(55L), eq("UPDATE"), old.capture(), updated.capture(),
                eq(101L), eq("course-test"));
        assertThat(old.getValue()).contains("이전 운영내역", "2026-04-10", "managementItemCode", "attachmentIds");
        assertThat(updated.getValue()).contains("새 운영내역", "2025-12-31", "managementItemCode", "attachmentIds");
    }

    @Test
    void filteredMultiRoleListUsesSameCriteriaAndRolesForCount() throws Exception {
        CurrentUser multi = user(101L, "R01", "R02");
        perform(get("/api/business/course-operations").param("managementNo", " EDU-55 ")
                .param("page", "1").param("pageSize", "50"), multi)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        ArgumentCaptor<CourseOperationSearchCriteria> query =
                ArgumentCaptor.forClass(CourseOperationSearchCriteria.class);
        verify(mapper).list(query.capture(), eq(101L), eq(List.of("R01", "R02")));
        assertThat(query.getValue().managementNo()).isEqualTo("EDU-55");
        assertThat(query.getValue().offset()).isEqualTo(50);
        verify(mapper).count(eq(query.getValue()), eq(101L), eq(List.of("R01", "R02")));
    }

    @Test
    void detailUsesUnionOfReadScopes() throws Exception {
        persisted = row(102L, "DRAFT", "학과 실적", LocalDate.now());
        when(scopes.countSharedActiveOrganization(101L, 102L)).thenReturn(1);
        perform(get("/api/business/course-operations/55"), user(101L, "R01", "R02"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.teacherUserId").value(102));
    }

    @Test
    void certificationReadScopeIsApplied() throws Exception {
        persisted = row(102L, "DRAFT", "인증 범위 실적", LocalDate.now());
        when(scopes.countCertificationScope(104L, 102L)).thenReturn(1);
        perform(get("/api/business/course-operations/55"), user(104L, "R04"))
                .andExpect(status().isOk());
    }

    @Test
    void foreignDetailIsForbidden() throws Exception {
        persisted = row(102L, "DRAFT", "비공개 실적", LocalDate.now());
        perform(get("/api/business/course-operations/55"), faculty)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void foreignUpdateIsForbiddenAndUnchanged() throws Exception {
        persisted = row(102L, "DRAFT", "이전 운영내역", LocalDate.now());
        perform(put("/api/business/course-operations/55").content(BODY), faculty)
                .andExpect(status().isForbidden());
        assertThat(persisted.performanceDetails()).isEqualTo("이전 운영내역");
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void confirmedUpdateReturnsTypedConflictAndNoMutation() throws Exception {
        persisted = row(101L, "EVALUATION_CONFIRMED", "확정 원본", LocalDate.now());
        perform(put("/api/business/course-operations/55").content(BODY), faculty)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        assertThat(persisted.performanceDetails()).isEqualTo("확정 원본");
        verify(mapper, never()).updateHeader(anyMap());
        verify(mapper, never()).insertHistory(any(), any(), any(), any(), any(), any());
    }

    @Test
    void submittedUpdateIsLocked() throws Exception {
        persisted = row(101L, "SUBMITTED", "제출 원본", LocalDate.now());
        perform(put("/api/business/course-operations/55").content(BODY), faculty)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void finalizedEvaluationBlocksCreateBeforeHeaderInsert() throws Exception {
        when(mapper.lockFinalizations(101L, "2026")).thenReturn(List.of(1L));
        perform(post("/api/business/course-operations").content(BODY), faculty)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void inactivePeriodBlocksCreate() throws Exception {
        when(scopes.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        perform(post("/api/business/course-operations").content(BODY), faculty)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void inactivePeriodBlocksUpdateAndPreservesOriginal() throws Exception {
        when(scopes.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        perform(put("/api/business/course-operations/55").content(BODY), faculty)
                .andExpect(status().isConflict());
        assertThat(persisted.performanceDetails()).isEqualTo("이전 운영내역");
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void unavailableTeacherEditableSettingBlocksSave() throws Exception {
        when(mapper.allowedItem(anyLong(), anyString(), anyString())).thenReturn(0);
        perform(post("/api/business/course-operations").content(BODY), faculty)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.managementItemCode").exists());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void missingPerformanceDetailsRejectsCreateAndUpdate() throws Exception {
        String invalid = "{\"managementItemCode\":\"COURSE_OPERATION\",\"achievementDate\":\"2026-04-11\"}";
        perform(post("/api/business/course-operations").content(invalid), faculty)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.performanceDetails").exists());
        perform(put("/api/business/course-operations/55").content(invalid), faculty)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.performanceDetails").exists());
        verifyNoInteractions(mapper);
    }

    @Test
    void unauthorizedRoleDeniedForEveryOperation() throws Exception {
        CurrentUser outsider = user(107L, "R07");
        perform(get("/api/business/course-operations"), outsider).andExpect(status().isForbidden());
        perform(get("/api/business/course-operations/55"), outsider).andExpect(status().isForbidden());
        perform(post("/api/business/course-operations").content(BODY), outsider).andExpect(status().isForbidden());
        perform(put("/api/business/course-operations/55").content(BODY), outsider).andExpect(status().isForbidden());
        verifyNoInteractions(mapper);
    }

    @Test
    void readOnlyBusinessRoleCannotWrite() throws Exception {
        perform(post("/api/business/course-operations").content(BODY), user(102L, "R02"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(mapper);
    }

    @Test
    void administratorOverrideKeepsScopeBypassButPeriodGuard() throws Exception {
        CurrentUser admin = user(109L, "R09");
        perform(get("/api/business/course-operations/55"), admin).andExpect(status().isOk());
        perform(put("/api/business/course-operations/55").content(BODY), admin).andExpect(status().isOk());
        verify(scopes).countActiveInputPeriods("2026", 101L);
    }

    @Test
    void anonymousIsUnauthenticated() throws Exception {
        mvc.perform(get("/api/business/course-operations"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void missingDetailAndUpdateAreNotFound() throws Exception {
        perform(get("/api/business/course-operations/999"), faculty).andExpect(status().isNotFound());
        perform(put("/api/business/course-operations/999").content(BODY), faculty).andExpect(status().isNotFound());
    }

    @Test
    void unexpectedErrorNeverLeaksSensitiveMessage() throws Exception {
        when(mapper.find(55L, false)).thenThrow(new IllegalStateException("password=secret jdbc:postgresql"));
        perform(get("/api/business/course-operations/55"), faculty)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("password="))));
    }

    private org.springframework.test.web.servlet.ResultActions perform(
            MockHttpServletRequestBuilder request, CurrentUser user) throws Exception {
        return mvc.perform(request.requestAttr("currentUser", user).header("X-Request-Id", "course-test")
                .contentType(MediaType.APPLICATION_JSON));
    }

    private static CurrentUser user(Long id, String... roles) {
        return new CurrentUser(id, "faculty", "E" + id, "교원", List.of(roles), List.of());
    }

    private static CourseOperationRow row(Long owner, String state, String performance, LocalDate date) {
        return new CourseOperationRow(55L, "EDU-55", owner, "faculty", "KNUE-DEPT-COMP", "2026",
                "COURSE_OPERATION", date, performance, state, List.of("opaque-id"),
                LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-11T09:00:00"));
    }
}
