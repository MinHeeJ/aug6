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
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
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

/** HTTP tests reach the actual service; only DB adapter and shared period guard are mocked. */
@WebMvcTest(LectureImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({LectureImprovementService.class, LectureImprovementExceptionAdvice.class})
class LectureImprovementApiContractTest {
    @Autowired MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @MockBean LectureImprovementMapper mapper;
    @MockBean EducationAchievementGuardService guard;
    private final CurrentUser faculty = user(101L, "R01");
    private static final String PATH = "/api/business/lecture-improvements";
    private static final String BODY = """
            {"managementItemCode":"LECTURE_IMPROVEMENT","achievementDate":"2026-04-10",
             "academicYear":2025,"semester":1,"achievementContent":"개선 내용","attachmentIds":["ref"]}
            """;

    @BeforeEach
    void setup() {
        when(mapper.academicYears()).thenReturn(List.of("2025", "2026"));
        when(mapper.semesters()).thenReturn(List.of("1", "2"));
        when(mapper.allowedItem(any(), any(), any())).thenReturn(1);
        when(mapper.organization(101L)).thenReturn("KNUE-DEPT-COMP");
        when(guard.validateMutation(any(), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.find(9L)).thenReturn(row(101L, "DRAFT", "2025", "1"));
        when(mapper.lock(9L)).thenReturn(row(101L, "DRAFT", "2025", "1"));
        when(mapper.canRead(any(), any(), any())).thenReturn(1);
        when(mapper.updateHeader(any())).thenReturn(1);
        doAnswer(invocation -> {
            Map<String, Object> values = invocation.getArgument(0);
            values.put("id", 9L);
            return null;
        }).when(mapper).insertHeader(any());
    }

    @Test
    void contractIsLoadedFromClasspath() throws Exception {
        String fixture = new String(new ClassPathResource("contracts/openapi.yaml").getContentAsByteArray(),
                java.nio.charset.StandardCharsets.UTF_8);
        assertThat(fixture).contains("createLectureImprovement", "updateLectureImprovement", PATH);
    }

    @Test
    void createUsesGeneratedKeyThenDetailAndHistoryAndDetailReadback() throws Exception {
        mvc.perform(post(PATH).requestAttr("currentUser", faculty).header("X-Request-Id", "trace-create")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.academicYear").value("2025"))
                .andExpect(jsonPath("$.data.achievement.semester").value("1"))
                .andExpect(jsonPath("$.meta.requestId").value("trace-create"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(any());
        order.verify(mapper).insertDetail(argThat(values -> Long.valueOf(9).equals(values.get("id"))));
        order.verify(mapper).statusHistory(any());
        order.verify(mapper).find(9L);
        order.verify(mapper).history(argThat(v -> v.get("before") == null
                && v.get("after").toString().contains("academicYear")
                && v.get("requestId").equals("trace-create")));
        mvc.perform(get(PATH + "/9").requestAttr("currentUser", faculty))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.semester").value("1"));
    }

    @Test
    void updateRetainsEvaluationYearAndWritesEveryBeforeAndAfterField() throws Exception {
        when(mapper.find(9L)).thenReturn(row(101L, "DRAFT", "2026", "2"));
        when(guard.validateMutation(any(), any())).thenReturn(OccurredDateValidation.outsideEvaluationPeriod());
        mvc.perform(put(PATH + "/9").requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("2026-04-10", "2027-04-10").replace("2025", "2026")
                        .replace("\"semester\":1", "\"semester\":2")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.achievement.academicYear").value("2026"))
                .andExpect(jsonPath("$.data.achievement.semester").value("2"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        verify(guard).validateMutation(eq(faculty), eq(new EducationAchievementMutationContext(
                101L, "2026", LocalDate.parse("2027-04-10"))));
        verify(mapper).history(argThat(v -> v.get("before").toString().contains("1")
                && v.get("after").toString().contains("2")
                && v.get("before").toString().contains("attachmentIds")
                && v.get("after").toString().contains("managementItemCode")));
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void filteredListAndCountReceiveIdenticalUnionScopeAndFilters() throws Exception {
        CurrentUser multi = user(101L, "R01", "R02", "R04");
        when(mapper.list(any(), eq(101L), eq(multi.roles())))
                .thenReturn(List.of(row(102L, "DRAFT", "2025", "1")));
        when(mapper.count(any(), eq(101L), eq(multi.roles()))).thenReturn(1L);
        mvc.perform(get(PATH).param("academicYear", "2025").param("semester", "1")
                .requestAttr("currentUser", multi))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].teacherUserId").value(102));
        var criteria = new LectureImprovementSearchCriteria(0, 20, 0L, null, "2025", "1");
        verify(mapper).list(criteria, 101L, multi.roles());
        verify(mapper).count(criteria, 101L, multi.roles());
    }

    @ParameterizedTest
    @ValueSource(strings = {"academicYear", "semester", "managementItemCode", "achievementContent"})
    void missingRequiredFieldIs400(String field) throws Exception {
        var payload = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(BODY);
        payload.remove(field);
        mvc.perform(post(PATH).requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields." + field).exists())
                .andExpect(jsonPath("$.meta.requestId").exists());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void invalidSemesterIs400BeforeMutation() throws Exception {
        mvc.perform(put(PATH + "/9").requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("\"semester\":1", "\"semester\":3")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.semester").exists());
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void invalidAcademicYearIs400() throws Exception {
        mvc.perform(post(PATH).requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("2025", "9999")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.academicYear").exists());
    }

    @Test
    void unavailableEditableItemIs400() throws Exception {
        when(mapper.allowedItem(any(), any(), any())).thenReturn(0);
        mvc.perform(post(PATH).requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.managementItemCode").exists());
        verify(mapper, never()).insertHeader(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED"})
    void nonEditableStateRefusesUpdateWithoutChangingOriginal(String state) throws Exception {
        var original = row(101L, state, "2025", "1");
        when(mapper.lock(9L)).thenReturn(original);
        mvc.perform(put(PATH + "/9").requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code")
                        .value(state.equals("EVALUATION_CONFIRMED") ? "CONFIRMED_DATA_LOCKED" : "INVALID_STATE_TRANSITION"));
        verify(mapper, never()).updateHeader(any());
        verify(mapper, never()).updateDetail(any());
        verify(mapper, never()).history(any());
        assertThat(original.semester()).isEqualTo("1");
    }

    @Test
    void inactivePeriodIs409WithoutWrites() throws Exception {
        when(guard.validateMutation(any(), any())).thenThrow(new ConflictException("PERIOD_NOT_ACTIVE: closed"));
        mvc.perform(put(PATH + "/9").requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void othersUpdateIsForbiddenBeforePeriodOrWrites() throws Exception {
        when(mapper.lock(9L)).thenReturn(row(102L, "DRAFT", "2025", "1"));
        mvc.perform(put(PATH + "/9").requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isForbidden());
        verifyNoInteractions(guard);
        verify(mapper, never()).updateHeader(any());
    }

    @Test
    void outOfScopeDetailIsForbidden() throws Exception {
        when(mapper.canRead(any(), any(), any())).thenReturn(0);
        mvc.perform(get(PATH + "/9").requestAttr("currentUser", faculty)).andExpect(status().isForbidden());
    }

    @Test
    void missingDetailAndUpdateAre404() throws Exception {
        mvc.perform(get(PATH + "/999").requestAttr("currentUser", faculty)).andExpect(status().isNotFound());
        mvc.perform(put(PATH + "/999").requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isNotFound());
    }

    @Test
    void eachOperationRejectsUnadmittedRole() throws Exception {
        var denied = user(107L, "R07");
        mvc.perform(get(PATH).requestAttr("currentUser", denied)).andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/9").requestAttr("currentUser", denied)).andExpect(status().isForbidden());
        mvc.perform(post(PATH).requestAttr("currentUser", denied).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(put(PATH + "/9").requestAttr("currentUser", denied).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void readersCannotCreateOrUpdate() throws Exception {
        for (String role : List.of("R02", "R04")) {
            mvc.perform(post(PATH).requestAttr("currentUser", user(102L, role))
                    .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
            mvc.perform(put(PATH + "/9").requestAttr("currentUser", user(102L, role))
                    .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        }
    }

    @Test
    void adminBypassDoesNotBypassFinalizationGuard() throws Exception {
        mvc.perform(get(PATH + "/9").requestAttr("currentUser", user(1L, "R09"))).andExpect(status().isOk());
        when(guard.validateMutation(any(), any())).thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED"));
        mvc.perform(put(PATH + "/9").requestAttr("currentUser", user(1L, "R09"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isConflict());
    }

    @Test
    void anonymousRequestIs401() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPageSizeIs400() throws Exception {
        mvc.perform(get(PATH).param("pageSize", "10").requestAttr("currentUser", faculty))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adapterFailureNeverLeaksSqlAndDoesNotWriteHistory() throws Exception {
        doThrow(new IllegalStateException("secret SQL password")).when(mapper).insertDetail(any());
        mvc.perform(post(PATH).requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.message").value("오류가 발생했습니다. 관리자에게 문의하세요."));
        verify(mapper, never()).history(any());
    }

    private static CurrentUser user(Long id, String... roles) {
        return new CurrentUser(id, "faculty", "E0101", "교원", List.of(roles), List.of());
    }

    private static LectureImprovementRow row(Long owner, String status, String year, String semester) {
        return new LectureImprovementRow(9L, "EDU-test", owner, "KNUE-DEPT-COMP", "2026",
                "LECTURE_IMPROVEMENT", LocalDate.parse("2026-04-10"), status, year, semester, "개선 내용", List.of("ref"));
    }
}
