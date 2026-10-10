package kr.ac.knue.commonfoundation.lectureimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
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

/** HTTP tests keep controller and service real; retained writes prove mocked-boundary readback, not SQL durability. */
@WebMvcTest(LectureImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({LectureImprovementService.class, GlobalExceptionHandler.class, LectureImprovementExceptionHandler.class})
class LectureImprovementApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean LectureImprovementMapper mapper;
    @MockBean EducationAchievementGuardMapper guard;
    @MockBean FunctionPermissionService permissions;

    private LectureImprovementRow stored;
    private Map<String, Object> header;
    private String oldSnapshot;
    private String newSnapshot;
    private final CurrentUser faculty = user("R01");

    @BeforeEach
    void setupRetainedPersistenceBoundary() {
        stored = row(101L, "DRAFT", "기존 내용", "2026", "2025-1");
        header = null;
        oldSnapshot = null;
        newSnapshot = null;
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.validManagementItem(anyString(), anyString())).thenReturn(1);
        when(mapper.validSemester(anyString(), anyString())).thenReturn(1);
        when(mapper.organization(anyLong())).thenReturn("KNUE-DEPT-COMP");
        when(mapper.canRead(anyLong(), anyLong(), any())).thenReturn(1);
        when(mapper.find(eq(41L), anyBoolean())).thenAnswer(call -> stored);
        when(mapper.list(any(), anyLong(), any())).thenAnswer(call -> List.of(stored));
        when(mapper.count(any(), anyLong(), any())).thenReturn(1L);
        when(mapper.semesters()).thenReturn(List.of(Map.of("code", "2025-1", "name", "1학기")));
        when(mapper.managementItems(any())).thenReturn(List.of(Map.of("code", "LECTURE_IMPROVEMENT", "name", "강의개선")));
        doAnswer(call -> {
            header = call.getArgument(0);
            header.put("achievementId", 41L);
            stored = null;
            return null;
        }).when(mapper).insertHeader(any());
        doAnswer(call -> {
            header = call.getArgument(0);
            return 1;
        }).when(mapper).updateHeader(any());
        doAnswer(call -> {
            retainDetail(call.getArgument(1));
            return null;
        }).when(mapper).insertDetail(anyLong(), any());
        doAnswer(call -> {
            retainDetail(call.getArgument(1));
            return null;
        }).when(mapper).updateDetail(anyLong(), any());
        doAnswer(call -> {
            oldSnapshot = call.getArgument(1);
            newSnapshot = call.getArgument(2);
            return null;
        }).when(mapper).history(anyLong(), any(), anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void createThenGetRetainsAcademicYearSemesterAndCreateHistory() throws Exception {
        try (var stream = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThat(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .contains("operationId: createLectureImprovement", "operationId: getLectureImprovement");
        }
        mvc.perform(write(post("/api/business/lecture-improvements"), body(), faculty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.academicYear").value("2025"))
                .andExpect(jsonPath("$.data.achievement.semester").value("2025-2"))
                .andExpect(jsonPath("$.meta.requestId").value("lecture-test"));
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 41L).requestAttr("currentUser", faculty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceContent").value("변경 내용"));
        assertThat(oldSnapshot).isNull();
        assertThat(json.readTree(newSnapshot).get("academicYear").asText()).isEqualTo("2025");
        var order = org.mockito.Mockito.inOrder(mapper);
        order.verify(mapper).insertHeader(any());
        order.verify(mapper).insertDetail(eq(41L), any());
        order.verify(mapper).find(41L, false);
        verify(mapper).statusHistory(41L, null, "DRAFT", 101L, "lecture-test");
    }

    @Test
    void updatePreservesYearAndAuditsEveryChangedFieldThenReadsBack() throws Exception {
        String before = json.writeValueAsString(stored);
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), body(), faculty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.achievement.semester").value("2025-2"));
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 41L).requestAttr("currentUser", faculty))
                .andExpect(jsonPath("$.data.performanceContent").value("변경 내용"));
        assertThat(json.readTree(oldSnapshot)).isEqualTo(json.readTree(before));
        assertThat(json.readTree(newSnapshot).get("performanceContent").asText()).isEqualTo("변경 내용");
        assertThat(header.get("year")).isEqualTo("2026");
        assertThat(header.get("achievementId")).isEqualTo(41L);
    }

    @Test
    void occurrenceOutsideEvaluationPeriodWarnsAndRetainsExistingYear() throws Exception {
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(0);
        var request = body();
        request.put("achievementDate", "2027-01-03");
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), request, faculty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"));
        verify(guard).countActiveInputPeriods("2026", 101L);
    }

    @Test
    void listAndCountReceiveIdenticalFiltersAndAllRoleScopes() throws Exception {
        CurrentUser multi = user("R01", "R02", "R04");
        mvc.perform(get("/api/business/lecture-improvements").requestAttr("currentUser", multi)
                        .param("managementNo", " LI-001 ").param("page", "2").param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(50));
        var capture = ArgumentCaptor.forClass(LectureImprovementSearchCriteria.class);
        verify(mapper).list(capture.capture(), eq(101L), eq(multi.roles()));
        verify(mapper).count(eq(capture.getValue()), eq(101L), eq(multi.roles()));
        assertThat(capture.getValue().rowOffset()).isEqualTo(100);
        assertThat(capture.getValue().managementNo()).isEqualTo("LI-001");
    }

    @Test
    void detailForScopedReaderSucceeds() throws Exception {
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 41L).requestAttr("currentUser", user("R02")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementId").value(41));
    }

    @Test
    void detailOutsideScopeIsForbidden() throws Exception {
        when(mapper.canRead(anyLong(), anyLong(), any())).thenReturn(0);
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 41L).requestAttr("currentUser", faculty))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailMissingIsNotFound() throws Exception {
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 999L).requestAttr("currentUser", faculty))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void updateMissingIsNotFoundAndHasNoWrites() throws Exception {
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 999L), body(), faculty))
                .andExpect(status().isNotFound());
        noWrites();
    }

    @Test
    void createMissingAcademicYearIsBadRequest() throws Exception {
        var request = body(); request.remove("academicYear");
        mvc.perform(write(post("/api/business/lecture-improvements"), request, faculty))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("academicYear"));
        noWrites();
    }

    @Test
    void createMissingSemesterIsBadRequest() throws Exception {
        var request = body(); request.remove("semester");
        mvc.perform(write(post("/api/business/lecture-improvements"), request, faculty))
                .andExpect(status().isBadRequest());
        noWrites();
    }

    @Test
    void updateInactiveSemesterIsBadRequest() throws Exception {
        when(mapper.validSemester(anyString(), anyString())).thenReturn(0);
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), body(), faculty))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("semester"));
        noWrites();
    }

    @Test
    void createInvalidManagementItemIsBadRequest() throws Exception {
        when(mapper.validManagementItem(anyString(), anyString())).thenReturn(0);
        mvc.perform(write(post("/api/business/lecture-improvements"), body(), faculty))
                .andExpect(status().isBadRequest());
        noWrites();
    }

    @Test
    void createUnownedAttachmentIsBadRequest() throws Exception {
        var request = body(); request.put("attachmentRef", "another-users-token");
        mvc.perform(write(post("/api/business/lecture-improvements"), request, faculty))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("attachmentRef"));
        noWrites();
    }

    @Test
    void updateCannotChangeEvaluationIdentity() throws Exception {
        var request = body(); request.put("evaluationYear", "2027");
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), request, faculty))
                .andExpect(status().isBadRequest());
        noWrites();
    }

    @Test
    void createCannotInjectConfirmedState() throws Exception {
        var request = body(); request.put("achievementStatus", "EVALUATION_CONFIRMED");
        mvc.perform(write(post("/api/business/lecture-improvements"), request, faculty))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
        noWrites();
    }

    @Test
    void updateCanSubmitDraftAndWritesStatusHistory() throws Exception {
        var request = body(); request.put("achievementStatus", "SUBMITTED");
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), request, faculty))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementStatus").value("SUBMITTED"));
        verify(mapper).statusHistory(41L, "DRAFT", "SUBMITTED", 101L, "lecture-test");
    }

    @Test
    void updateSubmittedRowIsConflictAndUnchanged() throws Exception {
        stored = row(101L, "SUBMITTED", "기존 내용", "2026", "2025-1");
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), body(), faculty))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
        assertThat(stored.performanceContent()).isEqualTo("기존 내용");
        noWrites();
    }

    @Test
    void updateConfirmedRowIsConflictAndUnchanged() throws Exception {
        stored = row(101L, "EVALUATION_CONFIRMED", "기존 내용", "2026", "2025-1");
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), body(), faculty))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"))
                .andExpect(jsonPath("$.meta.requestId").value("lecture-test"));
        assertThat(stored.performanceContent()).isEqualTo("기존 내용");
        noWrites();
    }

    @Test
    void updateFinalizationLocksDraftToo() throws Exception {
        when(guard.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), body(), faculty))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        noWrites();
    }

    @Test
    void createOutsideInputPeriodIsConflict() throws Exception {
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(write(post("/api/business/lecture-improvements"), body(), faculty))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        noWrites();
    }

    @Test
    void updateOutsideInputPeriodIsConflictAndUnchanged() throws Exception {
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), body(), faculty))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        assertThat(stored.performanceContent()).isEqualTo("기존 내용");
        noWrites();
    }

    @Test
    void updateOtherOwnerIsForbiddenBeforePeriodChecks() throws Exception {
        stored = row(202L, "DRAFT", "다른 교원", "2026", "2025-1");
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), body(), faculty))
                .andExpect(status().isForbidden());
        verify(guard, never()).countActiveInputPeriods(anyString(), anyLong());
        noWrites();
    }

    @Test
    void createNonFacultyRoleIsForbidden() throws Exception {
        mvc.perform(write(post("/api/business/lecture-improvements"), body(), user("R02")))
                .andExpect(status().isForbidden());
        noWrites();
    }

    @Test
    void updateNonFacultyRoleIsForbidden() throws Exception {
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), body(), user("R04")))
                .andExpect(status().isForbidden());
        noWrites();
    }

    @Test
    void listUnapprovedRoleIsForbidden() throws Exception {
        mvc.perform(get("/api/business/lecture-improvements").requestAttr("currentUser", user("R07")))
                .andExpect(status().isForbidden());
        verify(mapper, never()).list(any(), anyLong(), any());
    }

    @Test
    void detailUnapprovedRoleIsForbidden() throws Exception {
        mvc.perform(get("/api/business/lecture-improvements/{achievementId}", 41L).requestAttr("currentUser", user("R07")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousListIsUnauthorized() throws Exception {
        mvc.perform(get("/api/business/lecture-improvements")).andExpect(status().isUnauthorized());
    }

    @Test
    void createFunctionDeniedDoesNotWrite() throws Exception {
        when(permissions.evaluate(any())).thenThrow(new ForbiddenException());
        mvc.perform(write(post("/api/business/lecture-improvements"), body(), faculty))
                .andExpect(status().isForbidden());
        noWrites();
    }

    @Test
    void adminBypassCanReadCreateAndUpdateWithoutBusinessRole() throws Exception {
        CurrentUser admin = user("R09");
        mvc.perform(get("/api/business/lecture-improvements").requestAttr("currentUser", admin))
                .andExpect(status().isOk());
        mvc.perform(write(post("/api/business/lecture-improvements"), body(), admin)).andExpect(status().isOk());
        mvc.perform(write(put("/api/business/lecture-improvements/{achievementId}", 41L), body(), admin))
                .andExpect(status().isOk());
        verify(permissions, never()).evaluate(any());
    }

    @Test
    void unexpectedPersistenceFailureDoesNotLeakSqlOrPaths() throws Exception {
        doThrow(new IllegalStateException("sensitive SQL credential path"))
                .when(mapper).insertDetail(anyLong(), any());
        String response = mvc.perform(write(post("/api/business/lecture-improvements"), body(), faculty))
                .andExpect(status().isInternalServerError()).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("sensitive SQL", "credential path");
        verify(mapper, never()).history(anyLong(), any(), anyString(), anyString(), anyLong(), anyString());
    }

    private void noWrites() {
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateHeader(any());
        verify(mapper, never()).history(anyLong(), any(), anyString(), anyString(), anyLong(), anyString());
    }

    private MockHttpServletRequestBuilder write(
            MockHttpServletRequestBuilder builder, Map<String, Object> body, CurrentUser actor) throws Exception {
        return builder.requestAttr("currentUser", actor).header("X-Request-Id", "lecture-test")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    private Map<String, Object> body() {
        return new java.util.HashMap<>(Map.of(
                "managementItemCode", "LECTURE_IMPROVEMENT", "achievementDate", "2026-04-10",
                "performanceContent", "변경 내용", "academicYear", "2025", "semester", "2025-2"));
    }

    private void retainDetail(LectureImprovementRequest body) {
        stored = new LectureImprovementRow(41L, "LI-001", (Long) header.get("owner"), "교원", "KNUE-DEPT-COMP",
                (String) header.get("year"), (String) header.get("code"), (LocalDate) header.get("date"),
                body.performanceContent(), body.academicYear(), body.semester(), (String) header.get("status"),
                (String) header.get("attachment"), LocalDateTime.of(2026, 4, 10, 9, 0), LocalDateTime.now());
    }

    private static CurrentUser user(String... roles) {
        return new CurrentUser(101L, "faculty", "E101", "교원", List.of(roles), List.of());
    }

    private static LectureImprovementRow row(Long owner, String state, String content, String year, String semester) {
        return new LectureImprovementRow(41L, "LI-001", owner, "교원", "KNUE-DEPT-COMP", year,
                "LECTURE_IMPROVEMENT", LocalDate.of(2026, 4, 10), content, "2025", semester, state, null,
                LocalDateTime.of(2026, 4, 10, 9, 0), LocalDateTime.of(2026, 4, 10, 9, 0));
    }
}
