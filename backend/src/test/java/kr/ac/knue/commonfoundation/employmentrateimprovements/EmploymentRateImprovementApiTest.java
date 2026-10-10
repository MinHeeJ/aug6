package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Real HTTP/controller/service tests with state retained exclusively from mapper write arguments. */
@WebMvcTest(EmploymentRateImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EmploymentRateImprovementService.class,
        EmploymentRateImprovementExceptionHandler.class})
class EmploymentRateImprovementApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean EmploymentRateImprovementMapper mapper;
    @MockBean EducationAchievementGuardMapper guard;
    private final CurrentUser teacher = actor(101L, "R01");
    private final AtomicReference<EmploymentRateImprovementRow> stored = new AtomicReference<>();
    private final String body = """
            {"managementItemCode":"EMPLOYMENT_RATE_IMPROVEMENT","achievementDate":"2026-04-10",
             "specialLectureStartDate":"2026-04-10","specialLectureEndDate":"2026-04-17",
             "mockExamQuestionPeriod":"모의고사 출제기간"}
            """;

    @BeforeEach
    void setup() {
        stored.set(row(101L, "DRAFT", "원본", "2026"));
        when(mapper.find(eq(81L), anyBoolean())).thenAnswer(call -> stored.get());
        when(mapper.visible(eq(81L), anyLong(), anyList())).thenReturn(1);
        when(mapper.managementItemCodes()).thenReturn(List.of("EMPLOYMENT_RATE_IMPROVEMENT"));
        when(mapper.functionAllowed(anyList(), anyString())).thenReturn(1);
        when(mapper.organization(anyLong())).thenReturn("KNUE-DEPT-COMP");
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            values.put("id", 81L);
            return null;
        }).when(mapper).insertHeader(anyMap());
        doAnswer(call -> {
            EmploymentRateImprovementRequest request = call.getArgument(1);
            stored.set(from(request, stored.get().achievementStatus(), stored.get().evaluationYear()));
            return null;
        }).when(mapper).insertDetail(anyLong(), any());
        doAnswer(call -> {
            EmploymentRateImprovementRequest request = call.getArgument(1);
            stored.set(from(request, call.getArgument(2), stored.get().evaluationYear()));
            return null;
        }).when(mapper).updateHeader(anyLong(), any(), anyString(), anyLong());
    }

    @Test
    void createReadsBackActualWrittenDetailAndAuditsSnapshot() throws Exception {
        mvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", teacher).header("X-Request-Id", "create-trace")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementId").value(81))
                .andExpect(jsonPath("$.meta.requestId").value("create-trace"));
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mockExamQuestionPeriod").value("모의고사 출제기간"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(anyMap());
        order.verify(mapper).insertDetail(eq(81L), any());
        order.verify(mapper).find(81L, false);
        verify(mapper).history(eq(81L), eq("CREATE"), isNull(), contains("specialLectureEndDate"),
                eq(101L), eq("create-trace"));
    }

    @Test
    void updatePreservesYearAndAuditsAllChangedFields() throws Exception {
        when(guard.countEvaluationDatePeriods(eq("2026"), eq(101L), any())).thenReturn(0);
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).header("X-Request-Id", "update-trace")
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("2026-04-10", "2025-12-31")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementDate").value("2025-12-31"));
        verify(mapper).find(81L, true);
        verify(mapper).history(eq(81L), eq("UPDATE"), contains("원본"),
                contains("2025-12-31"), eq(101L), eq("update-trace"));
    }

    @Test
    void listFilteredScopedRoleReceivesMatchingTotal() throws Exception {
        CurrentUser chair = actor(101L, "R02");
        when(mapper.list(any(), eq(101L), eq(chair.roles()))).thenReturn(List.of(stored.get()));
        when(mapper.count(any(), eq(101L), eq(chair.roles()))).thenReturn(1L);
        mvc.perform(get("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", chair).param("managementNo", "ERI-001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("ERI-001"));
        verify(mapper).count(argThat(c -> "ERI-001".equals(c.managementNo())), eq(101L), eq(List.of("R02")));
    }

    @Test
    void multiRoleDetailUsesUnionRoles() throws Exception {
        CurrentUser multiple = new CurrentUser(101L, "professor1", "E0101", "교원",
                List.of("R01", "R02", "R04"), List.of());
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", multiple))
                .andExpect(status().isOk());
        verify(mapper).visible(81L, 101L, multiple.roles());
    }

    @Test
    void missingManagementItemOnCreateIsFieldError() throws Exception {
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"managementItemCode\":\"EMPLOYMENT_RATE_IMPROVEMENT\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void missingManagementItemOnUpdateIsFieldError() throws Exception {
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"managementItemCode\":\"EMPLOYMENT_RATE_IMPROVEMENT\",", "")))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).updateHeader(anyLong(), any(), anyString(), anyLong());
    }

    @Test
    void reversedLecturePeriodIsRejected() throws Exception {
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("2026-04-17", "2026-04-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("specialLectureEndDate"));
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void inactiveManagementItemIsRejected() throws Exception {
        when(mapper.managementItemCodes()).thenReturn(List.of("OTHER"));
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void r07CreateIsForbidden() throws Exception {
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", actor(107L, "R07"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void r02UpdateIsForbidden() throws Exception {
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", actor(101L, "R02"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        unchanged();
    }

    @Test
    void otherOwnerCannotUpdate() throws Exception {
        stored.set(row(102L, "DRAFT", "원본", "2026"));
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        unchanged();
    }

    @Test
    void outsideScopeDetailIsForbidden() throws Exception {
        when(mapper.visible(anyLong(), anyLong(), anyList())).thenReturn(0);
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isForbidden());
    }

    @Test
    void absentDetailIs404() throws Exception {
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 999)
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isNotFound());
    }

    @Test
    void absentUpdateIs404() throws Exception {
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 999)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        unchanged();
    }

    @Test
    void confirmedRowUpdateLeavesOriginalUnchanged() throws Exception {
        stored.set(row(101L, "EVALUATION_CONFIRMED", "원본", "2026"));
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        unchanged();
    }

    @Test
    void finalizedTargetCreateMakesNoWrites() throws Exception {
        when(guard.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void inactivePeriodCreateMakesNoWrites() throws Exception {
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void inactivePeriodUpdateLeavesOriginalUnchanged() throws Exception {
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).header("X-Request-Id", "period-trace")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"))
                .andExpect(jsonPath("$.meta.requestId").value("period-trace"));
        unchanged();
    }

    @Test
    void submittedRowCannotEdit() throws Exception {
        stored.set(row(101L, "SUBMITTED", "원본", "2026"));
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
        unchanged();
    }

    @Test
    void cannotSetConfirmedStatusThroughCreate() throws Exception {
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"mockExamQuestionPeriod\"",
                                "\"achievementStatus\":\"CERTIFIED\",\"mockExamQuestionPeriod\"")))
                .andExpect(status().isConflict());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void functionDenialMakesNoWrites() throws Exception {
        when(mapper.functionAllowed(anyList(), anyString())).thenReturn(0);
        mvc.perform(post("/api/business/employment-rate-improvements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void anonymousListIs401() throws Exception {
        mvc.perform(get("/api/business/employment-rate-improvements"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administratorCanUseReadAndUpdate() throws Exception {
        CurrentUser admin = actor(1L, "R09");
        mvc.perform(get("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", admin))
                .andExpect(status().isOk());
        mvc.perform(put("/api/business/employment-rate-improvements/{achievementId}", 81)
                        .requestAttr("currentUser", admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void durableContractIsAvailableOnClasspath() throws Exception {
        try (var stream = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThat(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .contains("operationId: createEmploymentRateImprovement",
                            "operationId: updateEmploymentRateImprovement");
        }
    }

    private void unchanged() {
        assertThat(stored.get().mockExamQuestionPeriod()).isEqualTo("원본");
        verify(mapper, never()).updateHeader(anyLong(), any(), anyString(), anyLong());
        verify(mapper, never()).history(anyLong(), anyString(), any(), any(), anyLong(), anyString());
    }

    private EmploymentRateImprovementRow from(EmploymentRateImprovementRequest body, String status, String year) {
        EmploymentRateImprovementRow old = stored.get();
        return new EmploymentRateImprovementRow(81L, old.managementNo(), old.teacherUserId(), old.teacherName(),
                old.organizationCode(), year, body.managementItemCode(), body.achievementDate(),
                body.specialLectureStartDate(), body.specialLectureEndDate(), body.mockExamQuestionPeriod(), status,
                body.attachmentRef(), old.createdAt(), old.createdBy(), old.updatedAt().plusSeconds(1), 101L);
    }

    private EmploymentRateImprovementRow row(Long owner, String status, String content, String year) {
        LocalDate date = LocalDate.parse("2026-04-10");
        LocalDateTime time = date.atStartOfDay();
        return new EmploymentRateImprovementRow(81L, "ERI-001", owner, "교원", "KNUE-DEPT-COMP", year,
                "EMPLOYMENT_RATE_IMPROVEMENT", date, date, date.plusDays(7), content, status, null,
                time, owner, time, owner);
    }

    private static CurrentUser actor(Long id, String role) {
        return new CurrentUser(id, "professor1", "E0101", "교원", List.of(role), List.of());
    }
}
