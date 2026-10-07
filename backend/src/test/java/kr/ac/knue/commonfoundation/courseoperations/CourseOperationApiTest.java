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
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementConflictAdvice;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionMapper;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP tests reach the real controller and service; only database adapters are mocked. */
@WebMvcTest(CourseOperationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EducationAchievementConflictAdvice.class,
        CourseOperationInputAdvice.class, CourseOperationService.class})
class CourseOperationApiTest {
    @Autowired private MockMvc mvc;
    @MockBean private CourseOperationMapper mapper;
    @MockBean private EducationAchievementGuardMapper guards;
    @MockBean private FunctionPermissionMapper permissions;
    private final CurrentUser owner = user(101L, List.of("R01"));
    private static final String ROOT = "/api/business/course-operations";
    private static final String BODY = """
            {"managementItemCode":"ATTENDANCE","achievementDate":"2026-04-10","performanceDetails":"운영 내역"}
            """;

    @BeforeEach
    void setup() {
        when(permissions.findByKey(anyString(), anyString(), anyString())).thenAnswer(call ->
                new FunctionPermissionRow(1L, call.getArgument(0), "강좌", call.getArgument(1), "역할",
                        call.getArgument(2), "ALLOW", null, LocalDateTime.now()));
        when(mapper.organizations(eq(101L), any())).thenReturn(List.of("DEPT"));
        when(mapper.managementItems("ATTENDANCE")).thenReturn(1);
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(mapper.insertHeader(any(), any(), any(), any(), any(), any())).thenReturn(82L);
        when(mapper.find(82L)).thenReturn(row(101L, "DRAFT", "운영 내역", "2026-04-10"));
        when(mapper.lock(82L)).thenReturn(row(101L, "DRAFT", "이전 내역", "2026-04-09"));
        when(mapper.inScope(eq(82L), any(), any())).thenReturn(1);
    }

    @Test
    void createThenListAndDetailPreserveSavedContentAndAudit() throws Exception {
        mvc.perform(post(ROOT).requestAttr("currentUser", owner).header("X-Request-Id", " trace ")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.performanceDetails").value("운영 내역"))
                .andExpect(jsonPath("$.data.achievement.attachmentIds").isArray())
                .andExpect(jsonPath("$.meta.requestId").value("trace"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(any(), eq(101L), eq("DEPT"), eq("2026"), any(), eq("[]"));
        order.verify(mapper).insertDetail(82L, "운영 내역");
        order.verify(mapper).initialStatus(82L, 101L);
        verify(mapper).history(82L, "CREATE", "performance_detail", null, "운영 내역", 101L, "trace");
        when(mapper.list(any(), eq(101L), eq(List.of("R01"))))
                .thenReturn(List.of(row(101L, "DRAFT", "운영 내역", "2026-04-10")));
        when(mapper.count(any(), eq(101L), eq(List.of("R01")))).thenReturn(1L);
        mvc.perform(get(ROOT).requestAttr("currentUser", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].performanceDetails").value("운영 내역"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(get(ROOT + "/82").requestAttr("currentUser", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceDetails").value("운영 내역"));
    }

    @Test
    void updateKeepsEvaluationYearAndWarnsForChangedDateWithFullHistory() throws Exception {
        String body = BODY.replace("2026-04-10", "2025-12-31");
        when(mapper.find(82L)).thenReturn(row(101L, "DRAFT", "운영 내역", "2025-12-31"));
        mvc.perform(put(ROOT + "/82").requestAttr("currentUser", owner).header("X-Request-Id", "update")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.achievement.achievementDate").value("2025-12-31"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        verify(guards).countActiveInputPeriods("2026", 101L);
        verify(mapper).history(82L, "UPDATE", "achievement_date", "2026-04-09", "2025-12-31", 101L, "update");
        verify(mapper).history(82L, "UPDATE", "performance_detail", "이전 내역", "운영 내역", 101L, "update");
        verify(mapper).updateDetail(82L, "운영 내역");
        mvc.perform(get(ROOT + "/82").requestAttr("currentUser", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.performanceDetails").value("운영 내역"));
    }

    @Test
    void finalizedRowIsUnchangedAndReturnsExactConflictCode() throws Exception {
        CourseOperationRow original = row(101L, "EVALUATION_CONFIRMED", "이전 내역", "2026-04-09");
        when(mapper.lock(82L)).thenReturn(original);
        mvc.perform(put(ROOT + "/82").requestAttr("currentUser", owner).header("X-Request-Id", "locked")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"))
                .andExpect(jsonPath("$.meta.requestId").value("locked"));
        verify(mapper, never()).updateHeader(any(), any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any());
        verify(mapper, never()).history(any(), any(), any(), any(), any(), any(), any());
        assertThat(original.performanceDetails()).isEqualTo("이전 내역");
    }

    @Test
    void periodClosedRejectsBeforeAnyWrites() throws Exception {
        when(guards.countActiveInputPeriods("2026", 101L)).thenReturn(0);
        for (var command : List.of(post(ROOT), put(ROOT + "/82"))) {
            mvc.perform(command.requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                            .content(BODY)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        }
        verify(mapper, never()).insertHeader(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).updateHeader(any(), any(), any(), any());
    }

    @Test
    void missingContentIs400OnBothWriteOperations() throws Exception {
        for (var command : List.of(post(ROOT), put(ROOT + "/82"))) {
            mvc.perform(command.requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                            .content(BODY.replace(",\"performanceDetails\":\"운영 내역\"", "")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[?(@.field == 'performanceDetails')]").isNotEmpty());
        }
        verifyNoInteractions(mapper);
    }

    @Test
    void unauthorizedRolesForeignOwnerAndOutOfScopeReadsAreForbidden() throws Exception {
        CurrentUser r07 = user(107L, List.of("R07"));
        mvc.perform(get(ROOT).requestAttr("currentUser", r07)).andExpect(status().isForbidden());
        mvc.perform(get(ROOT + "/82").requestAttr("currentUser", r07)).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).requestAttr("currentUser", r07).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isForbidden());
        mvc.perform(put(ROOT + "/82").requestAttr("currentUser", user(102L, List.of("R01")))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        when(mapper.inScope(eq(82L), any(), any())).thenReturn(0);
        mvc.perform(get(ROOT + "/82").requestAttr("currentUser", owner)).andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(any(), any(), any(), any());
    }

    @Test
    void listAndCountUseIdenticalFiltersAndAllRoleBranches() throws Exception {
        CurrentUser union = user(101L, List.of("R01", "R02", "R04"));
        CourseOperationSearch expected = new CourseOperationSearch(1, 50, 50L, "ATTENDANCE", "DRAFT");
        when(mapper.list(expected, 101L, union.roles())).thenReturn(List.of());
        when(mapper.count(expected, 101L, union.roles())).thenReturn(53L);
        mvc.perform(get(ROOT).requestAttr("currentUser", union).param("page", "1").param("pageSize", "50")
                        .param("managementItemCode", "ATTENDANCE").param("achievementStatus", "DRAFT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(53));
        verify(mapper).list(expected, 101L, union.roles());
        verify(mapper).count(expected, 101L, union.roles());
    }

    @Test
    void unknownIdIs404AndUnverifiedAttachmentOrAmbiguousItemIs400() throws Exception {
        mvc.perform(get(ROOT + "/999").requestAttr("currentUser", owner)).andExpect(status().isNotFound());
        mvc.perform(put(ROOT + "/999").requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isNotFound());
        mvc.perform(post(ROOT).requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("}", ",\"attachmentIds\":[\"unowned\"]}")))
                .andExpect(status().isBadRequest());
        when(mapper.managementItems("ATTENDANCE")).thenReturn(2);
        mvc.perform(post(ROOT).requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(any(), any(), any(), any(), any(), any());
    }

    @Test
    void explicitFunctionDenyBlocksAndAdminBypassStillReads() throws Exception {
        when(permissions.findByKey(any(), eq("R01"), eq("READ"))).thenReturn(
                new FunctionPermissionRow(1L, "screen", "screen", "R01", "role", "READ", "DENY", null, null));
        mvc.perform(get(ROOT).requestAttr("currentUser", owner)).andExpect(status().isForbidden());
        mvc.perform(get(ROOT + "/82").requestAttr("currentUser", user(1L, List.of("R09"))))
                .andExpect(status().isOk());
    }

    @Test
    void malformedDateAndPathReturnSafe400WithoutLeakingTypes() throws Exception {
        mvc.perform(post(ROOT).requestAttr("currentUser", owner).header("X-Request-Id", "bad-date")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("2026-04-10", "not-a-date")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.meta.requestId").value("bad-date"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("java.time"))));
        mvc.perform(get(ROOT + "/not-an-id").requestAttr("currentUser", owner))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(mapper);
    }

    private static CurrentUser user(Long id, List<String> roles) {
        return new CurrentUser(id, "faculty", "E" + id, "교원", roles, List.of());
    }

    private static CourseOperationRow row(Long owner, String status, String details, String date) {
        return new CourseOperationRow(82L, "CO-fixture", owner, "교원", "DEPT", "2026", "ATTENDANCE",
                LocalDate.parse(date), details, status, "[]", LocalDateTime.now(), LocalDateTime.now());
    }
}
