package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.storage.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateResponse;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
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

/** Real controller/service HTTP contract; only persistence and shared I/O are mocked in this DB-free slice. */
@WebMvcTest(EmploymentRateImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EmploymentRateImprovementService.class})
class EmploymentRateImprovementApiTest {
    private static final String PATH = "/api/business/employment-rate-improvements";
    private static final String BODY = """
            {"managementItemCode":"employment-rate-improvements","achievementDate":"2026-04-10",
             "specialLectureStartDate":"2026-04-01","specialLectureEndDate":"2026-04-10",
             "mockExamQuestionPeriod":"4월","attachmentIds":[]}
            """;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean EmploymentRateImprovementMapper mapper;
    @MockBean EducationAchievementGuardMapper guard;
    @MockBean FunctionPermissionService functions;
    @MockBean FileStoragePort storage;
    private final CurrentUser owner = new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01"), List.of());

    @BeforeEach
    void setup() {
        when(functions.evaluate(any())).thenAnswer(invocation -> {
            var request = (kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest)
                    invocation.getArgument(0);
            return new FunctionPermissionEvaluateResponse(true, request.screenId(), request.roleCode(),
                    request.functionType(), "ALLOW");
        });
        when(mapper.organizations(anyLong())).thenReturn(List.of("DEPT"));
        when(mapper.managementItems(anyString(), anyString())).thenReturn(List.of(
                new EmploymentRateImprovementManagementItem("employment-rate-improvements", "취업률 제고")));
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.updateHeader(any())).thenReturn(1);
        doAnswer(invocation -> {
            Map<String, Object> values = invocation.getArgument(0);
            values.put("achievementId", 91L);
            return null;
        }).when(mapper).insertHeader(any());
    }

    @Test
    void createInsertsHeaderBeforeDetailAndRecordsCompleteSnapshot() throws Exception {
        when(mapper.find(91L, false)).thenReturn(row("DRAFT", 101L, "2026-04-10", "4월"));
        mvc.perform(post(PATH).requestAttr("currentUser", owner).header("X-Request-Id", "create-trace")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementId").value(91))
                .andExpect(jsonPath("$.data.achievement.specialLectureStartDate").value("2026-04-01"))
                .andExpect(jsonPath("$.meta.requestId").value("create-trace"));
        var order = org.mockito.Mockito.inOrder(mapper);
        order.verify(mapper).insertHeader(any());
        order.verify(mapper).insertDetail(eq(91L), any());
        order.verify(mapper).insertStatus(91L, 101L, "create-trace");
        order.verify(mapper).find(91L, false);
        ArgumentCaptor<String> after = ArgumentCaptor.forClass(String.class);
        verify(mapper).insertHistory(eq(91L), eq("CREATE"), isNull(), after.capture(), eq(101L), eq("create-trace"));
        assertThat(json.readTree(after.getValue()).get("specialLectureEndDate").asText()).isEqualTo("2026-04-10");
    }

    @Test
    void updateKeepsStoredYearAndAuditsOldAndNewWholeRowsThenDetailReadsBack() throws Exception {
        when(mapper.find(91L, true)).thenReturn(row("DRAFT", 101L, "2026-04-10", "4월"));
        when(mapper.find(91L, false)).thenReturn(row("DRAFT", 101L, "2025-12-31", "수정"));
        when(guard.countEvaluationDatePeriods(eq("2026"), eq(101L), eq(LocalDate.parse("2025-12-31"))))
                .thenReturn(0);
        mvc.perform(put(PATH + "/91").requestAttr("currentUser", owner)
                        .header("X-Request-Id", "update-trace").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"achievementDate\":\"2026-04-10\"",
                                "\"achievementDate\":\"2025-12-31\"").replace("4월", "수정")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        ArgumentCaptor<String> before = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> after = ArgumentCaptor.forClass(String.class);
        verify(mapper).insertHistory(eq(91L), eq("UPDATE"), before.capture(), after.capture(),
                eq(101L), eq("update-trace"));
        assertThat(json.readTree(before.getValue()).get("mockExamQuestionPeriod").asText()).isEqualTo("4월");
        assertThat(json.readTree(after.getValue()).get("mockExamQuestionPeriod").asText()).isEqualTo("수정");
        verify(mapper).managementItems("2026", "DEPT");
        mvc.perform(get(PATH + "/91").requestAttr("currentUser", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementDate").value("2025-12-31"));
    }

    @Test
    void confirmedRowRefusesUpdateBeforeFunctionEvaluationOrMutation() throws Exception {
        EmploymentRateImprovementRow original = row("EVALUATION_CONFIRMED", 101L, "2026-04-10", "원본");
        when(mapper.find(91L, true)).thenReturn(original);
        mvc.perform(put(PATH + "/91").requestAttr("currentUser", owner)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("CONFIRMED_DATA_LOCKED")));
        verify(functions, never()).evaluate(any());
        verify(mapper, never()).updateHeader(any());
        verify(mapper, never()).updateDetail(anyLong(), any());
        assertThat(original.mockExamQuestionPeriod()).isEqualTo("원본");
    }

    @Test
    void finalizationAndClosedInputPeriodPreventAllWrites() throws Exception {
        when(mapper.lockFinalizations(101L, "2026")).thenReturn(List.of(101L));
        mvc.perform(post(PATH).requestAttr("currentUser", owner)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
        when(guard.countActiveInputPeriods("2026", 101L)).thenReturn(0);
        mvc.perform(post(PATH).requestAttr("currentUser", owner)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("PERIOD_NOT_ACTIVE")));
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).insertHistory(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsMissingManagementItemForBothCreateAndUpdate() throws Exception {
        for (var request : List.of(post(PATH), put(PATH + "/91"))) {
            mvc.perform(request.requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"achievementDate\":\"2026-04-10\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        }
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void rejectsReversedAndHalfSpecifiedSpecialLectureDates() throws Exception {
        for (String body : List.of(BODY.replace("2026-04-01", "2026-04-11"),
                BODY.replace("\"2026-04-01\"", "null"))) {
            mvc.perform(post(PATH).requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[0].field").value("specialLectureEndDate"));
        }
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void rejectsUnknownOrAmbiguousItemAndNonexistentAttachment() throws Exception {
        when(mapper.managementItems("2026", "DEPT")).thenReturn(List.of());
        mvc.perform(post(PATH).requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
        when(mapper.managementItems("2026", "DEPT")).thenReturn(List.of(
                new EmploymentRateImprovementManagementItem("employment-rate-improvements", "취업률 제고")));
        mvc.perform(post(PATH).requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("[]", "[\"nonexistent-file\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("attachmentIds"));
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void allFourRoutesRejectR07AndUnauthenticatedUsers() throws Exception {
        CurrentUser r07 = new CurrentUser(107L, "operator", "E107", "담당자", List.of("R07"), List.of());
        for (var request : List.of(get(PATH), get(PATH + "/91"), post(PATH), put(PATH + "/91"))) {
            mvc.perform(request.requestAttr("currentUser", r07).contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        verify(mapper, never()).find(anyLong(), anyBoolean());
    }

    @Test
    void otherOwnerCannotUpdateOrReadWhileMissingRowReturns404() throws Exception {
        when(mapper.find(eq(91L), anyBoolean())).thenReturn(row("DRAFT", 202L, "2026-04-10", "원본"));
        mvc.perform(put(PATH + "/91").requestAttr("currentUser", owner)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/91").requestAttr("currentUser", owner)).andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/404").requestAttr("currentUser", owner)).andExpect(status().isNotFound());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void multiRoleReadsUnionScopeAndListCountReceiveIdenticalFilters() throws Exception {
        CurrentUser user = new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01", "R02"), List.of());
        when(mapper.find(91L, false)).thenReturn(row("DRAFT", 202L, "2026-04-10", "타인"));
        when(guard.countSharedActiveOrganization(101L, 202L)).thenReturn(1);
        mvc.perform(get(PATH + "/91").requestAttr("currentUser", user)).andExpect(status().isOk());
        when(mapper.list(any(), eq(101L), eq(user.roles())))
                .thenReturn(List.of(row("DRAFT", 202L, "2026-04-10", "타인")));
        when(mapper.count(any(), eq(101L), eq(user.roles()))).thenReturn(1L);
        mvc.perform(get(PATH).param("managementNo", "ERI-91").param("pageSize", "50")
                        .requestAttr("currentUser", user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].teacherUserId").value(202));
        ArgumentCaptor<EmploymentRateImprovementSearchCriteria> criteria =
                ArgumentCaptor.forClass(EmploymentRateImprovementSearchCriteria.class);
        verify(mapper).list(criteria.capture(), eq(101L), eq(user.roles()));
        verify(mapper).count(eq(criteria.getValue()), eq(101L), eq(user.roles()));
        assertThat(criteria.getValue().managementNo()).isEqualTo("ERI-91");
    }

    @Test
    void administratorBypassRemainsAllowedButFunctionDenialBlocksBusinessRole() throws Exception {
        CurrentUser admin = new CurrentUser(1L, "admin", null, "관리자", List.of("R09"), List.of());
        when(mapper.find(eq(91L), anyBoolean())).thenReturn(row("DRAFT", 202L, "2026-04-10", "원본"));
        mvc.perform(get(PATH + "/91").requestAttr("currentUser", admin)).andExpect(status().isOk());
        mvc.perform(put(PATH + "/91").requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk());
        org.mockito.Mockito.doThrow(new ForbiddenException()).when(functions).evaluate(any());
        mvc.perform(post(PATH).requestAttr("currentUser", owner)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }

    private EmploymentRateImprovementRow row(String state, Long teacher, String date, String period) {
        return new EmploymentRateImprovementRow(91L, "ERI-91", teacher, "교원", "DEPT", "2026",
                "employment-rate-improvements", LocalDate.parse(date), state,
                LocalDate.parse("2026-04-01"), LocalDate.parse("2026-04-10"), period, List.of(),
                LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}
