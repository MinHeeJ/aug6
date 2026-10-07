package kr.ac.knue.commonfoundation.courseoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Actual HTTP/controller/service/guard coverage; only the database adapter is replaced in this MVC slice. */
@WebMvcTest(CourseOperationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, CourseOperationExceptionHandler.class,
        CourseOperationService.class, EducationAchievementGuardService.class})
class CourseOperationContractTest {
    private static final String PATH = "/api/business/course-operations";
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @MockBean
    private CourseOperationMapper mapper;
    @MockBean
    private EducationAchievementGuardMapper guard;
    private final CurrentUser owner = user(101L, List.of("R01"));
    private final AtomicReference<CourseOperationRow> stored = new AtomicReference<>();

    @BeforeEach
    void setup() {
        stored.set(row("DRAFT", 101L, "기존 실적", "2026", "2026-04-10"));
        when(guard.countActiveInputPeriods(any(), any())).thenReturn(1);
        when(guard.countEvaluationDatePeriods(any(), any(), any())).thenReturn(1);
        when(mapper.organization(101L)).thenReturn("KNUE-DEPT-COMP");
        when(mapper.countManagementItem("COURSE_OPERATION")).thenReturn(1);
        when(mapper.find(eq(31L), anyBoolean())).thenAnswer(call -> stored.get());
        when(mapper.updateHeader(any())).thenReturn(1);
        when(mapper.updateDetail(any())).thenAnswer(call -> {
            Map<String, Object> command = call.getArgument(0);
            CourseOperationRow old = stored.get();
            stored.set(row(old.achievementStatus(), old.teacherUserId(),
                    (String) command.get("performanceDetails"), old.evaluationYear(),
                    command.get("achievementDate").toString()));
            return 1;
        });
        doAnswer(call -> {
            Map<String, Object> command = call.getArgument(0);
            command.put("achievementId", 31L);
            stored.set(null); // Joined detail cannot exist before insertDetail.
            return null;
        }).when(mapper).insertHeader(any());
        doAnswer(call -> {
            Map<String, Object> command = call.getArgument(0);
            assertThat(command.get("achievementId")).isEqualTo(31L);
            stored.set(row("DRAFT", 101L, (String) command.get("performanceDetails"),
                    (String) command.get("evaluationYear"), command.get("achievementDate").toString()));
            return null;
        }).when(mapper).insertDetail(any());
        when(mapper.list(any(), anyLong(), any(), anyLong())).thenAnswer(call -> List.of(stored.get()));
        when(mapper.count(any(), anyLong(), any())).thenReturn(1L);
    }

    @Test
    void createListAndDetailAgreeAndHistoryUsesTheSameRequestId() throws Exception {
        mvc.perform(post(PATH).requestAttr("currentUser", owner).header("X-Request-Id", "course-create")
                        .contentType(MediaType.APPLICATION_JSON).content(body("운영 실적", "2026-04-10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.performanceDetails").value("운영 실적"))
                .andExpect(jsonPath("$.data.achievement.achievementStatus").value("DRAFT"))
                .andExpect(jsonPath("$.meta.requestId").value("course-create"));
        mvc.perform(get(PATH).requestAttr("currentUser", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].performanceDetails").value("운영 실적"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(get(PATH + "/31").requestAttr("currentUser", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceDetails").value("운영 실적"));
        InOrder order = Mockito.inOrder(mapper);
        order.verify(mapper).insertHeader(any());
        order.verify(mapper).insertDetail(any());
        order.verify(mapper).insertStatusHistory(any());
        order.verify(mapper).find(31L, false);
        ArgumentCaptor<Map<String, Object>> audit = mapCaptor();
        verify(mapper).insertChangeHistory(audit.capture());
        assertThat(audit.getValue().get("requestId")).isEqualTo("course-create");
        assertThat(audit.getValue().get("beforeValue")).isNull();
        assertThat(json.readTree(audit.getValue().get("afterValue").toString()).get("performanceDetails").asText())
                .isEqualTo("운영 실적");
    }

    @Test
    void updatePreservesYearAndRecordsAllOldAndNewValuesWithWarning() throws Exception {
        when(guard.countEvaluationDatePeriods(eq("2026"), eq(101L), any())).thenReturn(0);
        mvc.perform(put(PATH + "/31").requestAttr("currentUser", owner).header("X-Request-Id", "course-update")
                        .contentType(MediaType.APPLICATION_JSON).content(body("수정 실적", "2027-01-03")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        verify(guard).countActiveInputPeriods("2026", 101L);
        verify(mapper).find(31L, true);
        mvc.perform(get(PATH + "/31").requestAttr("currentUser", owner))
                .andExpect(jsonPath("$.data.performanceDetails").value("수정 실적"))
                .andExpect(jsonPath("$.data.achievementDate").value("2027-01-03"));
        ArgumentCaptor<Map<String, Object>> audit = mapCaptor();
        verify(mapper).insertChangeHistory(audit.capture());
        var before = json.readTree(audit.getValue().get("beforeValue").toString());
        var after = json.readTree(audit.getValue().get("afterValue").toString());
        assertThat(before.get("performanceDetails").asText()).isEqualTo("기존 실적");
        assertThat(after.get("performanceDetails").asText()).isEqualTo("수정 실적");
        assertThat(before.get("achievementDate").asText()).isEqualTo("2026-04-10");
        assertThat(after.get("achievementDate").asText()).isEqualTo("2027-01-03");
        assertThat(after.get("evaluationYear")).isEqualTo(before.get("evaluationYear"));
    }

    @Test
    void missingPerformanceDetailsIs400OnBothWriteOperations() throws Exception {
        String invalid = "{\"managementItemCode\":\"COURSE_OPERATION\",\"achievementDate\":\"2026-04-10\"}";
        for (var request : List.of(post(PATH), put(PATH + "/31"))) {
            mvc.perform(request.requestAttr("currentUser", owner).header("X-Request-Id", "invalid-course")
                            .contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.fields[?(@.field == 'performanceDetails')]").isNotEmpty())
                    .andExpect(jsonPath("$.meta.requestId").value("invalid-course"));
        }
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void unauthenticatedAndNonWriterRolesCannotMutate() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        for (String role : List.of("R02", "R04", "R07", "R09")) {
            for (var request : List.of(post(PATH), put(PATH + "/31"))) {
                mvc.perform(request.requestAttr("currentUser", user(101L, List.of(role)))
                                .contentType(MediaType.APPLICATION_JSON).content(body("실적", "2026-04-10")))
                        .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
            }
        }
        verify(mapper, never()).updateHeader(any());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void otherOwnerDetailAndUpdateAre403ButMultiRoleScopeIsUnion() throws Exception {
        stored.set(row("DRAFT", 102L, "다른 교원", "2026", "2026-04-10"));
        mvc.perform(get(PATH + "/31").requestAttr("currentUser", owner)).andExpect(status().isForbidden());
        mvc.perform(put(PATH + "/31").requestAttr("currentUser", owner)
                        .contentType(MediaType.APPLICATION_JSON).content(body("변경", "2026-04-10")))
                .andExpect(status().isForbidden());
        when(guard.countSharedActiveOrganization(101L, 102L)).thenReturn(1);
        mvc.perform(get(PATH + "/31").requestAttr("currentUser", user(101L, List.of("R01", "R02"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.teacherUserId").value(102));
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void everyNoneditableStatusIs409AndOriginalRemainsUnchanged() throws Exception {
        for (String state : List.of("SUBMITTED", "DEPARTMENT_CONFIRMED", "CERTIFIED", "EVALUATION_CONFIRMED")) {
            CourseOperationRow original = row(state, 101L, "원본", "2026", "2026-04-10");
            stored.set(original);
            mvc.perform(put(PATH + "/31").requestAttr("currentUser", owner)
                            .contentType(MediaType.APPLICATION_JSON).content(body("변경", "2026-04-10")))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFLICT"));
            assertThat(stored.get()).isEqualTo(original);
        }
        verify(mapper, never()).updateHeader(any());
        verify(mapper, never()).insertChangeHistory(any());
    }

    @Test
    void inactiveInputPeriodAndEvaluationFinalizationPreventWrites() throws Exception {
        when(guard.countActiveInputPeriods(any(), any())).thenReturn(0);
        mvc.perform(post(PATH).requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(body("실적", "2026-04-10")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.message").value(
                        org.hamcrest.Matchers.containsString("PERIOD_NOT_ACTIVE")));
        when(guard.countActiveInputPeriods(any(), any())).thenReturn(1);
        when(guard.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        mvc.perform(put(PATH + "/31").requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(body("실적", "2026-04-10")))
                .andExpect(status().isConflict());
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void filteredScopedListPassesIdenticalCriteriaToCountAndList() throws Exception {
        CurrentUser reader = user(101L, List.of("R02", "R04"));
        mvc.perform(get(PATH).requestAttr("currentUser", reader)
                        .param("managementItemCode", " COURSE_OPERATION ").param("page", "2")
                        .param("pageSize", "50").param("achievementStatus", "DRAFT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        CourseOperationSearchCriteria expected = new CourseOperationSearchCriteria(
                2, 50, null, null, "COURSE_OPERATION", "DRAFT");
        verify(mapper).list(expected, 101L, reader.roles(), 100L);
        verify(mapper).count(expected, 101L, reader.roles());
    }

    @Test
    void missingDetailAndInvalidPaginationAndUnknownManagementItem() throws Exception {
        when(mapper.find(999L, false)).thenReturn(null);
        mvc.perform(get(PATH + "/999").requestAttr("currentUser", owner)).andExpect(status().isNotFound());
        mvc.perform(get(PATH).requestAttr("currentUser", owner).param("pageSize", "10"))
                .andExpect(status().isBadRequest());
        when(mapper.countManagementItem("COURSE_OPERATION")).thenReturn(2);
        mvc.perform(post(PATH).requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(body("실적", "2026-04-10")))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void adapterFailureIsSanitizedAndCorrelated() throws Exception {
        Mockito.doThrow(new IllegalStateException("internal database failure"))
                .when(mapper).updateDetail(any());
        mvc.perform(put(PATH + "/31").requestAttr("currentUser", owner).header("X-Request-Id", "failed-course")
                        .contentType(MediaType.APPLICATION_JSON).content(body("실적", "2026-04-10")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret"))))
                .andExpect(jsonPath("$.meta.requestId").value("failed-course"));
        verify(mapper, never()).insertChangeHistory(any());
    }

    private String body(String details, String date) throws Exception {
        return json.writeValueAsString(new CourseOperationRequest("COURSE_OPERATION", LocalDate.parse(date),
                details, null, null));
    }

    private static CurrentUser user(Long id, List<String> roles) {
        return new CurrentUser(id, "faculty", "E0101", "교원", roles, List.of());
    }

    private static CourseOperationRow row(String status, Long ownerId, String details, String year, String date) {
        return new CourseOperationRow(31L, "CO-test", ownerId, "faculty", "KNUE-DEPT-COMP", year,
                "COURSE_OPERATION", LocalDate.parse(date), details, status, null,
                LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    }
}
