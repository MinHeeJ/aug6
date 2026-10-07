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
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.storage.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract tests reach the real service; database adapters alone are mocked in this database-free slice. */
@WebMvcTest(CourseOperationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, CourseOperationService.class})
class CourseOperationApiTest {
    @Autowired MockMvc mvc;
    @MockBean CourseOperationMapper mapper;
    @MockBean EducationAchievementGuardMapper guards;
    @MockBean FunctionPermissionService permissions;
    @MockBean FileStoragePort storage;
    private final CurrentUser teacher = user(101L, "R01");
    private static final String PATH = "/api/business/course-operations";
    private static final String BODY = """
            {"managementItemCode":"course-operations","achievementDate":"2027-01-01",
             "performanceDetails":"변경한 내역","attachmentIds":[]}
            """;

    @BeforeEach
    void setup() {
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(mapper.validManagementItem(anyString(), anyString(), anyLong())).thenReturn(1);
        when(mapper.organization(anyLong())).thenReturn("DEPT");
        when(mapper.inScope(anyLong(), anyLong(), anyList())).thenReturn(1);
        when(permissions.evaluate(any())).thenAnswer(call -> {
            var r = (kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest)
                    call.getArgument(0);
            return new FunctionPermissionEvaluateResponse(true, r.screenId(), r.roleCode(), r.functionType(), "ALLOW");
        });
    }

    @Test
    void createUsesGeneratedKeyThenDetailThenAuditAndReturnsPersistedRow() throws Exception {
        doAnswer(call -> {
            Map<String, Object> command = call.getArgument(0);
            command.put("id", 81L);
            return null;
        }).when(mapper).insertHeader(anyMap());
        when(mapper.find(81L, false)).thenReturn(row("DRAFT", "変경한 내역", 101L));
        mvc.perform(post(PATH).requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "course-create").content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementId").value(81))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("course-create"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(anyMap());
        order.verify(mapper).insertDetail(anyMap());
        order.verify(mapper).insertStatus(anyMap());
        order.verify(mapper).find(81L, false);
        order.verify(mapper).insertHistory(anyMap());
        ArgumentCaptor<Map<String, Object>> audit = ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHistory(audit.capture());
        assertThat(audit.getValue().get("before")).isNull();
        assertThat(audit.getValue().get("after").toString()).contains("performanceDetails", "attachmentIds");
    }

    @Test
    void updatePreservesStoredYearAndCompleteBeforeAfterHistory() throws Exception {
        CourseOperationRow before = row("DRAFT", "기존 내역", 101L);
        CourseOperationRow after = row("DRAFT", "변경한 내역", 101L);
        when(mapper.find(81L, true)).thenReturn(before);
        when(mapper.find(81L, false)).thenReturn(after);
        when(mapper.updateHeader(anyMap())).thenReturn(1);
        mvc.perform(put(PATH + "/81").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.achievement.performanceDetails").value("변경한 내역"));
        verify(guards).countActiveInputPeriods("2026", 101L);
        verify(mapper).validManagementItem("course-operations", "2026", 101L);
        ArgumentCaptor<Map<String, Object>> audit = ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHistory(audit.capture());
        assertThat(audit.getValue().get("before").toString()).contains("기존 내역", "2026", "attachmentIds");
        assertThat(audit.getValue().get("after").toString()).contains("변경한 내역", "2026");
        mvc.perform(get(PATH + "/81").requestAttr("currentUser", teacher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.performanceDetails").value("변경한 내역"));
    }

    @Test
    void filteredListAndCountUseIdenticalCriteriaAndMultiRoleUnion() throws Exception {
        CurrentUser multi = user(101L, "R01", "R02", "R04");
        when(mapper.list(any(), eq(101L), eq(multi.roles()), eq(0L)))
                .thenReturn(List.of(row("DRAFT", "기존 내역", 202L)));
        when(mapper.count(any(), eq(101L), eq(multi.roles()))).thenReturn(1L);
        mvc.perform(get(PATH).requestAttr("currentUser", multi).param("managementNo", " CO-81 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].teacherUserId").value(202))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        var criteria = new CourseOperationSearchCriteria(0, 20, "CO-81", null, null);
        verify(mapper).list(criteria, 101L, multi.roles(), 0L);
        verify(mapper).count(criteria, 101L, multi.roles());
    }

    @Test
    void detailRejectsOutOfScopeAndMissingIds() throws Exception {
        when(mapper.find(81L, false)).thenReturn(row("DRAFT", "기존 내역", 202L));
        when(mapper.inScope(81L, 101L, teacher.roles())).thenReturn(0);
        mvc.perform(get(PATH + "/81").requestAttr("currentUser", teacher)).andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/99").requestAttr("currentUser", teacher)).andExpect(status().isNotFound());
    }

    @Test
    void unauthorizedRolesCannotCallAnyOperationAndR02CannotWrite() throws Exception {
        CurrentUser denied = user(107L, "R07");
        mvc.perform(get(PATH).requestAttr("currentUser", denied)).andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/81").requestAttr("currentUser", denied)).andExpect(status().isForbidden());
        mvc.perform(post(PATH).requestAttr("currentUser", denied)
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mvc.perform(put(PATH + "/81").requestAttr("currentUser", user(102L, "R02"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        verifyNoInteractions(mapper);
    }

    @Test
    void missingPerformanceDetailsIs400OnBothWrites() throws Exception {
        String missing = """
                {"managementItemCode":"course-operations","achievementDate":"2026-04-10"}
                """;
        for (var builder : List.of(post(PATH), put(PATH + "/81"))) {
            mvc.perform(builder.requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON)
                            .content(missing))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[?(@.field == 'performanceDetails')]").isNotEmpty());
        }
        verifyNoInteractions(mapper);
    }

    @Test
    void confirmedAndSubmittedRowsRemainUnchangedAndPrecedeFunctionEvaluation() throws Exception {
        for (String state : List.of("EVALUATION_CONFIRMED", "SUBMITTED")) {
            when(mapper.find(81L, true)).thenReturn(row(state, "기존 내역", 101L));
            mvc.perform(put(PATH + "/81").requestAttr("currentUser", teacher)
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isConflict());
        }
        verify(mapper, never()).updateHeader(anyMap());
        verify(mapper, never()).updateDetail(anyMap());
        verify(mapper, never()).insertHistory(anyMap());
        verifyNoInteractions(permissions);
    }

    @Test
    void periodAndFinalizationConflictBlockAllWrites() throws Exception {
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(post(PATH).requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isConflict());
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guards.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        when(mapper.find(81L, true)).thenReturn(row("DRAFT", "기존 내역", 101L));
        mvc.perform(put(PATH + "/81").requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isConflict());
        verify(mapper, never()).insertHeader(anyMap());
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void foreignOwnerUpdateIsForbiddenBeforeMutation() throws Exception {
        when(mapper.find(81L, true)).thenReturn(row("DRAFT", "기존 내역", 202L));
        mvc.perform(put(PATH + "/81").requestAttr("currentUser", teacher)
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void administratorOverrideAllowsDetailAndUpdateWithoutChangingOwner() throws Exception {
        when(mapper.find(81L, true)).thenReturn(row("DRAFT", "기존 내역", 202L));
        when(mapper.find(81L, false)).thenReturn(row("DRAFT", "변경한 내역", 202L));
        when(mapper.updateHeader(anyMap())).thenReturn(1);
        mvc.perform(put(PATH + "/81").requestAttr("currentUser", user(1L, "R09"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk());
        verify(guards).countActiveInputPeriods("2026", 202L);
    }

    private static CurrentUser user(Long id, String... roles) {
        return new CurrentUser(id, "faculty", "E101", "교원", List.of(roles), List.of());
    }

    private static CourseOperationRow row(String status, String detail, Long owner) {
        return new CourseOperationRow(81L, "CO-81", owner, "교원", "DEPT", "2026", "course-operations",
                LocalDate.parse("2027-01-01"), detail, status, List.of(),
                LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}
