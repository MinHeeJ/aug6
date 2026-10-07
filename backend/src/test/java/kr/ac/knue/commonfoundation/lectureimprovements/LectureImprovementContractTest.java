package kr.ac.knue.commonfoundation.lectureimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

/** Real controller/service HTTP coverage; only DB adapters and the shared period guard are mocked. */
@WebMvcTest(LectureImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, LectureImprovementService.class})
class LectureImprovementContractTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private LectureImprovementMapper mapper;
    @MockBean
    private EducationAchievementGuardService guard;
    @MockBean
    private EducationAchievementGuardMapper scope;

    private static final String PATH = "/api/business/lecture-improvements";
    private static final String BODY = """
            {"managementItemCode":"FR-031","achievementDate":"2026-04-10",
             "performanceContent":"강의개선 내용","academicYear":"2025","semester":"1"}
            """;
    private final CurrentUser user = principal("R01");

    @BeforeEach
    void setup() {
        when(guard.validateMutation(any(), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.activeManagementItems("FR-031")).thenReturn(1);
        when(mapper.organization(101L)).thenReturn("TEST-DEPARTMENT");
        doAnswer(call -> {
            Map<String, Object> header = call.getArgument(0);
            header.put("id", 83L);
            return null;
        }).when(mapper).insertHeader(any());
    }

    @Test
    void createsHeaderThenDetailThenReadsAndAuditsTheSavedAcademicYear() throws Exception {
        when(mapper.find(83L)).thenReturn(row(101L, "DRAFT", "2025", "1", "강의개선 내용"));
        mvc.perform(post(PATH).requestAttr("currentUser", user).header("X-Request-Id", "lecture-save")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementId").value(83))
                .andExpect(jsonPath("$.data.achievement.academicYear").value("2025"))
                .andExpect(jsonPath("$.data.achievement.semester").value("1"))
                .andExpect(jsonPath("$.meta.requestId").value("lecture-save"));
        InOrder order = inOrder(mapper);
        order.verify(mapper).insertHeader(any());
        order.verify(mapper).insertDetail(eq(83L), any(), eq(101L));
        order.verify(mapper).statusHistory(83L, 101L, "lecture-save");
        order.verify(mapper).find(83L);
        ArgumentCaptor<String> snapshot = ArgumentCaptor.forClass(String.class);
        verify(mapper).changeHistory(eq(83L), eq("CREATE"), eq(null), snapshot.capture(),
                eq(101L), eq("lecture-save"));
        assertThat(snapshot.getValue()).contains("강의개선 내용", "2025", "managementItemCode", "achievementDate");
    }

    @Test
    void supportsApprovedFixtureNumericFieldsAndAchievementContentAlias() throws Exception {
        when(mapper.find(83L)).thenReturn(row(101L, "DRAFT", "2025", "1", "개선"));
        mvc.perform(post(PATH).requestAttr("currentUser", user).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"FR-031","achievementDate":"2026-04-10",
                                 "achievementContent":"개선","academicYear":2025,"semester":1,"attachmentIds":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.performanceContent").value("개선"));
    }

    @Test
    void updateKeepsEvaluationYearAndSnapshotsEveryChangedField() throws Exception {
        when(mapper.lock(83L)).thenReturn(row(101L, "DRAFT", "2025", "1", "이전 내용"));
        when(mapper.updateHeader(eq(83L), any(), any(), eq(101L), any())).thenReturn(1);
        LectureImprovementRow changed = new LectureImprovementRow(83L, "LI-test", 101L, "교원",
                "TEST-DEPARTMENT", "2026", "FR-031", LocalDate.of(2025, 12, 31), "DRAFT", null,
                "새 내용", "2024", "2", LocalDateTime.of(2026, 4, 10, 9, 0), LocalDateTime.of(2026, 4, 10, 10, 0));
        when(mapper.find(83L)).thenReturn(changed);
        when(guard.validateMutation(any(), any())).thenReturn(OccurredDateValidation.outsideEvaluationPeriod());
        mvc.perform(put(PATH + "/83").requestAttr("currentUser", user)
                        .header("X-Request-Id", "lecture-update").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("2026-04-10", "2025-12-31").replace("2025\"", "2024\"")
                                .replace("\"1\"", "\"2\"").replace("강의개선 내용", "새 내용")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.achievement.academicYear").value("2024"))
                .andExpect(jsonPath("$.data.achievement.semester").value("2"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        ArgumentCaptor<EducationAchievementMutationContext> context =
                ArgumentCaptor.forClass(EducationAchievementMutationContext.class);
        verify(guard).validateMutation(eq(user), context.capture());
        assertThat(context.getValue().evaluationYear()).isEqualTo("2026");
        ArgumentCaptor<String> before = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> after = ArgumentCaptor.forClass(String.class);
        verify(mapper).changeHistory(eq(83L), eq("UPDATE"), before.capture(), after.capture(),
                eq(101L), eq("lecture-update"));
        assertThat(before.getValue()).contains("이전 내용", "2025", "semester\":\"1");
        assertThat(after.getValue()).contains("새 내용", "2024", "2025-12-31", "semester\":\"2");
        mvc.perform(get(PATH + "/83").requestAttr("currentUser", user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.semester").value("2"));
    }

    @Test
    void filteredListPassesExactlyTheSameCriteriaAndUnionRolesToCount() throws Exception {
        CurrentUser multi = new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01", "R02"), List.of());
        when(mapper.list(any(), eq(101L), eq(multi.roles()))).thenReturn(List.of(row(102L, "DRAFT", "2025", "1", "내용")));
        when(mapper.count(any(), eq(101L), eq(multi.roles()))).thenReturn(1L);
        mvc.perform(get(PATH).requestAttr("currentUser", multi).param("pageSize", "50")
                        .param("academicYear", "2025").param("semester", "1").param("managementItemCode", " FR-031 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].userId").value(102));
        ArgumentCaptor<LectureImprovementSearchCriteria> criteria =
                ArgumentCaptor.forClass(LectureImprovementSearchCriteria.class);
        verify(mapper).list(criteria.capture(), eq(101L), eq(multi.roles()));
        verify(mapper).count(eq(criteria.getValue()), eq(101L), eq(multi.roles()));
        assertThat(criteria.getValue().managementItemCode()).isEqualTo("FR-031");
        assertThat(criteria.getValue().academicYear()).isEqualTo("2025");
        assertThat(criteria.getValue().pageSize()).isEqualTo(50);
        when(mapper.find(83L)).thenReturn(row(102L, "DRAFT", "2025", "1", "내용"));
        when(scope.countSharedActiveOrganization(101L, 102L)).thenReturn(1);
        mvc.perform(get(PATH + "/83").requestAttr("currentUser", multi)).andExpect(status().isOk());
    }

    @Test
    void confirmedAndSubmittedSourcesRemainUnchanged() throws Exception {
        for (String state : List.of("EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED")) {
            when(mapper.lock(83L)).thenReturn(row(101L, state, "2025", "1", "원본"));
            mvc.perform(put(PATH + "/83").requestAttr("currentUser", user).header("X-Request-Id", "lock-check")
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.meta.requestId").value("lock-check"));
        }
        verify(mapper, never()).updateHeader(any(), any(), any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any(), any());
        verify(mapper, never()).changeHistory(any(), any(), any(), any(), any(), any());
    }

    @Test
    void periodAndFinalizationGuardsPreventAllWrites() throws Exception {
        for (String reason : List.of("PERIOD_NOT_ACTIVE", "CONFIRMED_DATA_LOCKED")) {
            org.mockito.Mockito.doThrow(new ConflictException(reason))
                    .when(guard).validateMutation(any(), any());
            mvc.perform(post(PATH)
                        .requestAttr("currentUser", user).contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error.message").value(reason));
        }
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void rolesOwnershipAndNotFoundAreNotBypassed() throws Exception {
        for (String role : List.of("R02", "R04", "R07", "R09")) {
            mvc.perform(post(PATH).requestAttr("currentUser", principal(role))
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isForbidden());
            mvc.perform(put(PATH + "/83").requestAttr("currentUser", principal(role))
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isForbidden());
        }
        when(mapper.lock(83L)).thenReturn(row(102L, "DRAFT", "2025", "1", "원본"));
        mvc.perform(put(PATH + "/83").requestAttr("currentUser", user)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        when(mapper.find(83L)).thenReturn(row(102L, "DRAFT", "2025", "1", "원본"));
        mvc.perform(get(PATH + "/83").requestAttr("currentUser", user)).andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/404").requestAttr("currentUser", user)).andExpect(status().isNotFound());
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).requestAttr("currentUser", principal("R09"))).andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(any(), any(), any(), any(), any());
    }

    @Test
    void missingAcademicYearAndSemesterAndInvalidSemesterAreFieldErrors() throws Exception {
        for (String field : List.of("academicYear", "semester")) {
            String missing = BODY.replace(field.equals("academicYear") ? "\"academicYear\":\"2025\","
                    : ",\"semester\":\"1\"", "");
            mvc.perform(post(PATH)
                        .requestAttr("currentUser", user).contentType(MediaType.APPLICATION_JSON).content(missing))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[?(@.field == '" + field + "')]").isNotEmpty());
        }
        mvc.perform(put(PATH + "/83").requestAttr("currentUser", user).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"1\"", "\"3\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'semester')]").isNotEmpty());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void ambiguousManagementItemAndInvalidDateDoNotLeakOrMutate() throws Exception {
        when(mapper.activeManagementItems("FR-031")).thenReturn(2);
        mvc.perform(post(PATH).requestAttr("currentUser", user).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode"));
        mvc.perform(post(PATH).requestAttr("currentUser", user).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("2026-04-10", "not-a-date")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("입력값 형식을 확인하세요."));
        mvc.perform(get(PATH).requestAttr("currentUser", user).param("pageSize", "19"))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(any());
    }

    @Test
    void missingContentReportsBothApprovedNamesWithoutWriting() throws Exception {
        String missing = BODY.replace("\"performanceContent\":\"강의개선 내용\",", "");
        for (var builder : List.of(post(PATH), put(PATH + "/83"))) {
            mvc.perform(builder.requestAttr("currentUser", user)
                            .contentType(MediaType.APPLICATION_JSON).content(missing))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[?(@.field == 'achievementContent')]").isNotEmpty())
                    .andExpect(jsonPath("$.error.fields[?(@.field == 'performanceContent')]").isNotEmpty());
        }
        verify(mapper, never()).insertHeader(any());
        verify(mapper, never()).updateHeader(any(), any(), any(), any(), any());
    }

    @Test
    void approvedFixtureIncludesAllFourOperationIds() throws Exception {
        String contract = StreamUtils.copyToString(new ClassPathResource("contracts/openapi.yaml").getInputStream(),
                StandardCharsets.UTF_8);
        assertThat(contract).contains("operationId: listLectureImprovements", "operationId: getLectureImprovement",
                "operationId: createLectureImprovement", "operationId: updateLectureImprovement");
    }

    private static CurrentUser principal(String role) {
        return new CurrentUser(101L, "faculty", "E101", "교원", List.of(role), List.of());
    }

    private LectureImprovementRow row(Long owner, String status, String year, String semester, String content) {
        return new LectureImprovementRow(83L, "LI-test", owner, "교원", "TEST-DEPARTMENT", "2026", "FR-031",
                LocalDate.of(2026, 4, 10), status, null, content, year, semester,
                LocalDateTime.of(2026, 4, 10, 9, 0), LocalDateTime.of(2026, 4, 10, 9, 0));
    }
}
