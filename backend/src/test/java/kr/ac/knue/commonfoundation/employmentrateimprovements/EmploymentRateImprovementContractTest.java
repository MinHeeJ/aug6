package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
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
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Real controller and service HTTP contracts; persistence adapters are mocked, not the business decisions. */
@WebMvcTest(EmploymentRateImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EmploymentRateImprovementService.class})
class EmploymentRateImprovementContractTest {
    private static final String PATH = "/api/business/employment-rate-improvements";
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @MockBean
    private EmploymentRateImprovementMapper mapper;
    @MockBean
    private EducationAchievementGuardService guard;
    private final CurrentUser owner = user(101L, "R01");

    @BeforeEach
    void setup() {
        when(mapper.countManagementItem("FR-029")).thenReturn(1);
        when(mapper.organization(101L)).thenReturn("ORG");
        when(guard.validateMutation(any(), any())).thenReturn(OccurredDateValidation.accepted());
    }

    @Test
    void createUsesGeneratedKeyBeforeDetailAndPersistsSnapshotWithRequestId() throws Exception {
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            values.put("id", 88L);
            return 1;
        }).when(mapper).insertHeader(any());
        when(mapper.find(88L, false)).thenReturn(row(88L, 101L, "DRAFT", "2026", "2026-04-10", "저장됨"));
        mvc.perform(post(PATH).requestAttr("currentUser", owner)
                        .header("X-Request-Id", "CREATE-TRACE")
                        .contentType(MediaType.APPLICATION_JSON).content(body("2026-04-10", "저장됨")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementId").value(88))
                .andExpect(jsonPath("$.data.achievement.mockExamQuestionPeriod").value("저장됨"))
                .andExpect(jsonPath("$.data.achievement.attachmentIds[0]").value("file-token"))
                .andExpect(jsonPath("$.meta.requestId").value("CREATE-TRACE"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(any());
        order.verify(mapper).insertDetail(any());
        order.verify(mapper).insertStatusHistory(any());
        order.verify(mapper).find(88L, false);
        order.verify(mapper).insertChangeHistory(any());
        ArgumentCaptor<Map<String, Object>> captured = mapCaptor();
        verify(mapper).insertChangeHistory(captured.capture());
        assertThat(captured.getValue()).containsEntry("id", 88L).containsEntry("requestId", "CREATE-TRACE");
        assertThat(captured.getValue().get("before")).isNull();
        assertThat(json.readTree((String) captured.getValue().get("after")).path("specialLectureStartDate").asText())
                .isEqualTo("2026-04-10");
    }

    @Test
    void updateRetainsYearAndAuditsEveryChangedValueThenDetailReadsBack() throws Exception {
        var old = row(88L, 101L, "DRAFT", "2026", "2026-04-10", "기존");
        var saved = row(88L, 101L, "DRAFT", "2026", "2025-12-31", "변경");
        when(mapper.find(88L, true)).thenReturn(old);
        when(mapper.find(88L, false)).thenReturn(saved);
        when(mapper.updateHeader(any())).thenReturn(1);
        when(mapper.updateDetail(any())).thenReturn(1);
        when(mapper.countScope(88L, 101L, owner.roles())).thenReturn(1);
        when(guard.validateMutation(any(), any())).thenReturn(OccurredDateValidation.outsideEvaluationPeriod());
        mvc.perform(put(PATH + "/88").requestAttr("currentUser", owner)
                        .contentType(MediaType.APPLICATION_JSON).content(body("2025-12-31", "변경")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        verify(guard).validateMutation(owner,
                new EducationAchievementMutationContext(101L, "2026", LocalDate.parse("2025-12-31")));
        var audit = mapCaptor();
        verify(mapper).insertChangeHistory(audit.capture());
        var before = json.readTree((String) audit.getValue().get("before"));
        var after = json.readTree((String) audit.getValue().get("after"));
        assertThat(before.path("achievementDate").asText()).isEqualTo("2026-04-10");
        assertThat(after.path("achievementDate").asText()).isEqualTo("2025-12-31");
        assertThat(before.path("mockExamQuestionPeriod").asText()).isEqualTo("기존");
        assertThat(after.path("mockExamQuestionPeriod").asText()).isEqualTo("변경");
        assertThat(before.path("evaluationYear")).isEqualTo(after.path("evaluationYear"));
        mvc.perform(get(PATH + "/88").requestAttr("currentUser", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mockExamQuestionPeriod").value("변경"));
    }

    @Test
    void filteredMultiRoleListAndCountReceiveTheIdenticalPredicatesAndUnionRoles() throws Exception {
        var principal = user(101L, "R01", "R02", "R04");
        when(mapper.list(any(), eq(101L), eq(principal.roles())))
                .thenReturn(List.of(row(88L, 102L, "SUBMITTED", "2026", "2026-04-10", "타인")));
        when(mapper.count(any(), eq(101L), eq(principal.roles()))).thenReturn(1L);
        mvc.perform(get(PATH).requestAttr("currentUser", principal)
                        .param("managementNo", " EDU-88 ").param("page", "1").param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].teacherUserId").value(102))
                .andExpect(jsonPath("$.data.pageSize").value(50));
        ArgumentCaptor<EmploymentRateImprovementCriteria> criteria =
                ArgumentCaptor.forClass(EmploymentRateImprovementCriteria.class);
        verify(mapper).list(criteria.capture(), eq(101L), eq(principal.roles()));
        verify(mapper).count(eq(criteria.getValue()), eq(101L), eq(principal.roles()));
        assertThat(criteria.getValue().managementNo()).isEqualTo("EDU-88");
        assertThat(criteria.getValue().rowOffset()).isEqualTo(50L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED", "DEPARTMENT_CONFIRMED"})
    void lockedStatusesHaveNoWriteOrAuditSideEffects(String state) throws Exception {
        var original = row(88L, 101L, state, "2026", "2026-04-10", "원본");
        when(mapper.find(88L, true)).thenReturn(original);
        mvc.perform(put(PATH + "/88").requestAttr("currentUser", owner)
                        .header("X-Request-Id", "LOCK-TRACE")
                        .contentType(MediaType.APPLICATION_JSON).content(body("2026-04-10", "변경")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.meta.requestId").value("LOCK-TRACE"));
        verify(mapper, never()).updateHeader(any());
        verify(mapper, never()).updateDetail(any());
        verify(mapper, never()).insertChangeHistory(any());
        assertThat(original.mockExamQuestionPeriod()).isEqualTo("원본");
    }

    @Test
    void inactiveInputPeriodAndFinalizationRefuseCreateBeforeHeaderInsert() throws Exception {
        when(guard.validateMutation(any(), any())).thenThrow(new ConflictException("PERIOD_NOT_ACTIVE"));
        mvc.perform(post(PATH).requestAttr("currentUser", owner)
                        .contentType(MediaType.APPLICATION_JSON).content(body("2026-04-10", "변경")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("PERIOD_NOT_ACTIVE"));
        org.mockito.Mockito.doThrow(new ConflictException("CONFIRMED_DATA_LOCKED"))
                .when(guard).validateMutation(any(), any());
        mvc.perform(post(PATH).requestAttr("currentUser", owner)
                        .contentType(MediaType.APPLICATION_JSON).content(body("2026-04-10", "변경")))
                .andExpect(status().isConflict());
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).insertChangeHistory(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"R02", "R04", "R07", "R09"})
    void nonWriterRolesCannotCreateOrUpdate(String role) throws Exception {
        for (var request : List.of(post(PATH), put(PATH + "/88"))) {
            mvc.perform(request.requestAttr("currentUser", user(101L, role))
                            .contentType(MediaType.APPLICATION_JSON).content(body("2026-04-10", "변경")))
                    .andExpect(status().isForbidden());
        }
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).find(anyLong(), anyBoolean());
    }

    @Test
    void outsideOwnershipCannotReadOrUpdateAndMissingRowReturns404() throws Exception {
        when(mapper.find(88L, false)).thenReturn(row(88L, 102L, "DRAFT", "2026", "2026-04-10", "비공개"));
        when(mapper.find(88L, true)).thenReturn(row(88L, 102L, "DRAFT", "2026", "2026-04-10", "비공개"));
        mvc.perform(get(PATH + "/88").requestAttr("currentUser", owner))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(put(PATH + "/88").requestAttr("currentUser", user(101L, "R01", "R02"))
                        .contentType(MediaType.APPLICATION_JSON).content(body("2026-04-10", "변경")))
                .andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/999").requestAttr("currentUser", owner))
                .andExpect(status().isNotFound());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void missingManagementItemAndInvalidSpecialLectureRangeAreFieldErrors() throws Exception {
        for (var request : List.of(post(PATH), put(PATH + "/88"))) {
            mvc.perform(request.requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"achievementDate\":\"2026-04-10\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        }
        mvc.perform(post(PATH).requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"FR-029","achievementDate":"2026-04-10",
                                 "specialLectureStartDate":"2026-04-12","specialLectureEndDate":"2026-04-10"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'specialLectureEndDate')]").isNotEmpty());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void inactiveOrAmbiguousManagementItemIsRejected() throws Exception {
        when(mapper.countManagementItem("FR-029")).thenReturn(0, 2);
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post(PATH).requestAttr("currentUser", owner)
                            .contentType(MediaType.APPLICATION_JSON).content(body("2026-04-10", "변경")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode"));
        }
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void anonymousAndInvalidPaginationAreRejectedAndSensitiveFailuresAreMasked() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).requestAttr("currentUser", owner).param("pageSize", "30"))
                .andExpect(status().isBadRequest());
        when(mapper.list(any(), any(), any())).thenThrow(new IllegalStateException("password=secret SQL"));
        mvc.perform(get(PATH).requestAttr("currentUser", owner).header("X-Request-Id", "ERROR-TRACE"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.meta.requestId").value("ERROR-TRACE"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret"))));
    }

    @Test
    void approvedContractIsLoadedThroughTheClasspathWithoutEditingIt() throws Exception {
        try (var stream = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThat(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .contains("operationId: listEmploymentRateImprovements")
                    .contains("operationId: getEmploymentRateImprovement")
                    .contains("operationId: createEmploymentRateImprovement")
                    .contains("operationId: updateEmploymentRateImprovement");
        }
    }

    private String body(String date, String period) throws Exception {
        return json.writeValueAsString(Map.of(
                "managementItemCode", "FR-029", "achievementDate", date,
                "specialLectureStartDate", date, "specialLectureEndDate", date,
                "mockExamQuestionPeriod", period, "attachmentIds", List.of("file-token")));
    }

    private static CurrentUser user(Long id, String... roles) {
        return new CurrentUser(id, "faculty", "E101", "교원", List.of(roles), List.of());
    }

    private EmploymentRateImprovementStoredRow row(Long id, Long ownerId, String status,
            String year, String date, String period) {
        var day = LocalDate.parse(date);
        var time = LocalDateTime.parse("2026-04-10T09:00:00");
        return new EmploymentRateImprovementStoredRow(
                id, "EDU-" + id, ownerId, "교원", "ORG", year, "FR-029", day,
                status, day, day, period, "[\"file-token\"]", time, time);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass((Class) Map.class);
    }
}
