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
import kr.ac.knue.commonfoundation.common.EducationAchievementTestSupport;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionMapper;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Real HTTP controller/service path with mocked persistence; DB runtime is owned by the API runner. */
class LectureImprovementApiTest {
    private LectureImprovementMapper mapper;
    private EducationAchievementGuardMapper guard;
    private FunctionPermissionMapper permissions;
    private MockMvc mvc;
    private final CurrentUser owner = EducationAchievementTestSupport.principal(101L, "R01");
    private static final String ROOT = "/api/business/lecture-improvements";
    private static final String BODY = """
            {"managementItemCode":"EDU-ITEM","achievementDate":"2025-12-31",
             "achievementContent":"수업 개선","academicYear":2025,"semester":2,"attachmentIds":[]}
            """;

    @BeforeEach
    void setup() {
        mapper = mock(LectureImprovementMapper.class);
        guard = mock(EducationAchievementGuardMapper.class);
        permissions = mock(FunctionPermissionMapper.class);
        // Same standalone wiring and advice as the repository's foundation HTTP test.
        mvc = MockMvcBuilders.standaloneSetup(new LectureImprovementController(
                        new LectureImprovementService(mapper, guard, permissions)))
                .setControllerAdvice(new LectureImprovementErrorAdvice(),
                        new EducationAchievementConflictAdvice(), new GlobalExceptionHandler())
                .build();
        when(permissions.findByKey(anyString(), anyString(), anyString())).thenAnswer(call ->
                new FunctionPermissionRow(1L, call.getArgument(0), "강의개선", call.getArgument(1), "역할",
                        call.getArgument(2), "ALLOW", null, null));
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(mapper.managementItems("EDU-ITEM")).thenReturn(1);
        when(mapper.organizations(anyLong(), any())).thenReturn(List.of("DEPT"));
        when(mapper.find(41L)).thenReturn(row(101L, "DRAFT", 2025, 2));
        when(mapper.lock(41L)).thenReturn(row(101L, "DRAFT", 2024, 1));
        when(mapper.visible(eq(41L), anyLong(), anyList())).thenReturn(1);
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            values.put("id", 41L);
            return null;
        }).when(mapper).insertHeader(anyMap());
    }

    @Test
    void createUsesGeneratedKeyThenDetailAndFullHistoryAndReturnsStoredSemester() throws Exception {
        mvc.perform(post(ROOT).requestAttr("currentUser", owner).header("X-Request-Id", "  trace  ")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(2))
                .andExpect(jsonPath("$.data.achievement.attachmentIds").isArray())
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("trace"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(anyMap());
        order.verify(mapper).insertDetail(eq(41L), any(), eq("2"));
        order.verify(mapper).statusHistory(41L, 101L);
        order.verify(mapper).find(41L);
        verify(mapper).history(41L, "CREATE", "academic_year", null, "2025", 101L, "trace");
        verify(mapper).history(41L, "CREATE", "semester_code", null, "2", 101L, "trace");
        verify(mapper, never()).updateHeader(anyMap());
        mvc.perform(get(ROOT + "/41").requestAttr("currentUser", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.semester").value(2));
    }

    @Test
    void updateKeepsEvaluationYearAcrossDateChangeAndAuditsOldAndNewValues() throws Exception {
        mvc.perform(put(ROOT + "/41").requestAttr("currentUser", owner).header("X-Request-Id", "update")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(2));
        verify(guard).countActiveInputPeriods("2026", 101L);
        verify(guard).countEvaluationDatePeriods("2026", 101L, LocalDate.parse("2025-12-31"));
        verify(mapper).updateHeader(argThat(values -> "2026".equals(values.get("year"))));
        verify(mapper).updateDetail(eq(41L), any(), eq("2"));
        verify(mapper).history(41L, "UPDATE", "academic_year", "2024", "2025", 101L, "update");
        verify(mapper).history(41L, "UPDATE", "semester_code", "1", "2", 101L, "update");
    }

    @ParameterizedTest
    @ValueSource(strings = {"academicYear", "semester"})
    void missingRequiredYearOrSemesterIs400(String field) throws Exception {
        String body = BODY.replace(field.equals("academicYear") ? "\"academicYear\":2025," : "\"semester\":2,", "");
        mvc.perform(post(ROOT).requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == '" + field + "')]").isNotEmpty());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void invalidSemesterIs400() throws Exception {
        mvc.perform(put(ROOT + "/41").requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"semester\":2", "\"semester\":3")))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).lock(anyLong());
    }

    @Test
    void otherOwnerIs403AndNoRowsChange() throws Exception {
        when(mapper.lock(41L)).thenReturn(row(102L, "DRAFT", 2024, 1));
        mvc.perform(put(ROOT + "/41").requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)).andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(anyMap());
    }

    @ParameterizedTest
    @ValueSource(strings = {"R02", "R04", "R07"})
    void unauthorizedWriterIs403(String role) throws Exception {
        mvc.perform(post(ROOT).requestAttr("currentUser", EducationAchievementTestSupport.principal(101L, role))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void confirmedRecordAndClosedPeriodUseTyped409WithoutMutation() throws Exception {
        when(mapper.lock(41L)).thenReturn(row(101L, "EVALUATION_CONFIRMED", 2024, 1));
        mvc.perform(put(ROOT + "/41").requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        when(mapper.lock(41L)).thenReturn(row(101L, "DRAFT", 2024, 1));
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(put(ROOT + "/41").requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        verify(mapper, never()).updateHeader(anyMap());
        verify(mapper, never()).updateDetail(anyLong(), any(), anyString());
        verify(mapper, never()).history(anyLong(), anyString(), anyString(), any(), any(), anyLong(), anyString());
    }

    @Test
    void unknownUpdateIs404NotInsertAndDetailOutsideScopeIs403() throws Exception {
        mvc.perform(put(ROOT + "/99").requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)).andExpect(status().isNotFound());
        when(mapper.visible(eq(41L), anyLong(), anyList())).thenReturn(0);
        mvc.perform(get(ROOT + "/41").requestAttr("currentUser", owner)).andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void listAndCountReceiveSameFiltersAndUnionRoles() throws Exception {
        CurrentUser union = EducationAchievementTestSupport.principal(101L, "R01", "R02", "R04");
        var search = new LectureImprovementSearch(0, 20, 0, "EDU-ITEM", 2025, 2);
        when(mapper.list(search, 101L, union.roles())).thenReturn(List.of(row(102L, "DRAFT", 2025, 2)));
        when(mapper.count(search, 101L, union.roles())).thenReturn(1L);
        mvc.perform(get(ROOT).requestAttr("currentUser", union).param("managementItemCode", "EDU-ITEM")
                        .param("academicYear", "2025").param("semester", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].teacherUserId").value(102));
        verify(mapper).list(search, 101L, union.roles());
        verify(mapper).count(search, 101L, union.roles());
    }

    @Test
    void explicitFunctionDenyIs403AndAdministratorRemainsAllowed() throws Exception {
        when(permissions.findByKey(anyString(), anyString(), anyString())).thenReturn(
                new FunctionPermissionRow(1L, "screen", "screen", "R01", "role", "READ", "DENY", null, null));
        mvc.perform(get(ROOT + "/41").requestAttr("currentUser", owner)).andExpect(status().isForbidden());
        mvc.perform(get(ROOT + "/41").requestAttr("currentUser",
                        EducationAchievementTestSupport.principal(1L, "R09")))
                .andExpect(status().isOk());
    }

    @Test
    void fixtureAndSensitiveErrorEnvelope() throws Exception {
        assertThat(new ClassPathResource("contracts/openapi.yaml").getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8)).contains("operationId: createLectureImprovement");
        when(mapper.find(41L)).thenThrow(new IllegalStateException("secret-internal-SQL"));
        mvc.perform(get(ROOT + "/41").requestAttr("currentUser", owner))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.message").value(
                        "오류가 발생했습니다. 잠시 후 다시 시도하거나 관리자에게 문의하세요."));
    }

    @Test
    void malformedJsonAndPathTypesAre400WithoutParserLeaks() throws Exception {
        mvc.perform(post(ROOT).requestAttr("currentUser", owner).header("X-Request-Id", "bad-input")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("2025-12-31", "not-a-date")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.meta.requestId").value("bad-input"));
        mvc.perform(get(ROOT + "/not-an-id").requestAttr("currentUser", owner))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void submittedRowsAndFinalizationRecordsAreLockedWithoutWrite() throws Exception {
        when(mapper.lock(41L)).thenReturn(row(101L, "SUBMITTED", 2024, 1));
        mvc.perform(put(ROOT + "/41").requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)).andExpect(status().isConflict());
        when(mapper.lock(41L)).thenReturn(row(101L, "DRAFT", 2024, 1));
        when(guard.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        mvc.perform(put(ROOT + "/41").requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void ambiguousOrganizationsAndManagementCodesCannotInsert() throws Exception {
        when(mapper.organizations(anyLong(), any())).thenReturn(List.of("A", "B"));
        mvc.perform(post(ROOT).requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
        when(mapper.organizations(anyLong(), any())).thenReturn(List.of("A"));
        when(mapper.managementItems("EDU-ITEM")).thenReturn(2);
        mvc.perform(post(ROOT).requestAttr("currentUser", owner).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(anyMap());
    }

    private LectureImprovementRow row(Long ownerId, String status, Integer year, Integer semester) {
        return new LectureImprovementRow(41L, "LI-fixture", ownerId, "DEPT", "2026", "EDU-ITEM",
                LocalDate.parse("2025-12-31"), "수업 개선", year, semester, status, "[]");
    }
}
