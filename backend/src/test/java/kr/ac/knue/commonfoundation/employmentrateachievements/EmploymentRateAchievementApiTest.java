package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementResponseAdvice;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import kr.ac.knue.commonfoundation.storage.FileStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** Actual controller and service contract checks; persistence adapters are mocked only at the DB boundary. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EducationAchievementResponseAdvice.class, EmploymentRateAchievementService.class})
class EmploymentRateAchievementApiTest {
    private static final String BASE = "/api/business/employment-rate-achievements";
    private static final String BODY = """
            {"managementItemCode":"EMPLOYMENT_RATE_ACHIEVEMENT","achievementDate":"2025-12-20","achievementName":"새 실적"}
            """;
    @Autowired private MockMvc mvc;
    @MockBean private EmploymentRateAchievementMapper mapper;
    @MockBean private EducationAchievementGuardMapper guards;
    @MockBean private FileStoragePort storage;
    @MockBean private EmploymentRateExcelService excel;
    private Map<String, Object> original;

    @BeforeEach
    void setup() {
        original = row();
        when(mapper.find(10L, false)).thenAnswer(call -> new LinkedHashMap<>(original));
        when(mapper.find(10L, true)).thenAnswer(call -> new LinkedHashMap<>(original));
        when(mapper.visible(any(), eq(10L))).thenReturn(1);
        when(mapper.organizations(any())).thenReturn(List.of("DEPT"));
        when(mapper.years("DEPT")).thenReturn(List.of("2026"));
        when(mapper.managementItems(any())).thenReturn(List.of(Map.of(
                "managementItemCode", "EMPLOYMENT_RATE_ACHIEVEMENT", "teacherEditableYn", "Y")));
        when(guards.countActiveInputPeriods(any(), any())).thenReturn(1);
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            values.put("achievementId", 10L);
            original.putAll(values);
            return 1;
        }).when(mapper).insert(any());
        doAnswer(call -> {
            original.putAll(call.<Map<String, Object>>getArgument(0));
            return 1;
        }).when(mapper).update(any());
    }

    @Test
    void approvedClasspathFixtureContainsAllEightOperations() throws Exception {
        String contract = new String(new ClassPathResource("contracts/openapi.yaml").getInputStream().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        for (String operation : List.of("listEmploymentRateAchievements", "getEmploymentRateAchievement",
                "createEmploymentRateAchievement", "updateEmploymentRateAchievement", "downloadEmploymentRateAchievements",
                "uploadEmploymentRateAchievementsExcel", "createEmploymentRateBulkJob", "getEmploymentRateBulkJob")) {
            assertThat(contract).contains("operationId: " + operation);
        }
    }

    @Test
    void createThenListAndDetailAgreeAndAuditUsesGeneratedKey() throws Exception {
        mvc.perform(post(BASE).requestAttr("currentUser", user("R01")).header("X-Request-Id", "req-create")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementId").value(10))
                .andExpect(jsonPath("$.data.achievement.achievementName").value("새 실적"))
                .andExpect(jsonPath("$.meta.requestId").value("req-create"));
        when(mapper.list(any())).thenAnswer(call -> List.of(new LinkedHashMap<>(original)));
        when(mapper.count(any())).thenReturn(1L);
        mvc.perform(get(BASE).requestAttr("currentUser", user("R01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].achievementName").value("새 실적"));
        mvc.perform(get(BASE + "/10").requestAttr("currentUser", user("R01")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementName").value("새 실적"));
        verify(mapper).initialStatus(any());
        org.mockito.ArgumentCaptor<Map<String, Object>> captor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper, org.mockito.Mockito.times(4)).audit(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(entry -> {
            assertThat(entry.get("targetKey")).isEqualTo("10");
            assertThat(entry.get("requestId")).isEqualTo("req-create");
        });
    }

    @Test
    void updatePreservesYearAndAuditsEveryChangedField() throws Exception {
        mvc.perform(put(BASE + "/10").requestAttr("currentUser", user("R01"))
                        .header("X-Request-Id", "req-update").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        assertThat(original.get("evaluationYear")).isEqualTo("2026");
        org.mockito.ArgumentCaptor<Map<String, Object>> captor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper, org.mockito.Mockito.times(2)).audit(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(entry -> {
            assertThat(entry.get("requestId")).isEqualTo("req-update");
            assertThat(entry.get("beforeValue")).isNotNull();
            assertThat(entry.get("afterValue")).isNotNull();
        });
        mvc.perform(get(BASE + "/10").requestAttr("currentUser", user("R01")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementDate").value("2025-12-20"));
    }

    @Test
    void missingManagementItemIsObjectFieldError() throws Exception {
        mvc.perform(post(BASE).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2026-04-01\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.managementItemCode").exists());
        verify(mapper, never()).insert(any());
    }

    @Test
    void malformedDateIs400WithoutParserLeakage() throws Exception {
        mvc.perform(put(BASE + "/10").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("2025-12-20", "not-date")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(mapper, never()).update(any());
    }

    @Test
    void confirmedRecordRemainsUnchanged() throws Exception {
        original.put("achievementStatus", "EVALUATION_CONFIRMED");
        Map<String, Object> before = new LinkedHashMap<>(original);
        mvc.perform(put(BASE + "/10").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        assertThat(original).isEqualTo(before);
        verify(mapper, never()).update(any());
        verify(mapper, never()).audit(any());
    }

    @Test
    void submittedRecordCannotBeEdited() throws Exception {
        original.put("achievementStatus", "SUBMITTED");
        mvc.perform(put(BASE + "/10").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
        verify(mapper, never()).update(any());
    }

    @Test
    void inactivePeriodCannotChangeRow() throws Exception {
        when(guards.countActiveInputPeriods(any(), any())).thenReturn(0);
        mvc.perform(put(BASE + "/10").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        verify(mapper, never()).update(any());
    }

    @Test
    void anotherOwnerCannotUpdate() throws Exception {
        original.put("teacherUserId", 999L);
        mvc.perform(put(BASE + "/10").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(mapper, never()).update(any());
    }

    @Test
    void getEnforcesScopeAndMissingRowsAre404() throws Exception {
        when(mapper.visible(any(), eq(10L))).thenReturn(0);
        when(mapper.find(999L, false)).thenReturn(null);
        mvc.perform(get(BASE + "/10").requestAttr("currentUser", user("R02"))).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/999").requestAttr("currentUser", user("R01"))).andExpect(status().isNotFound());
    }

    @Test
    void listPassesAllRolesAndFilterToBothRowsAndTotal() throws Exception {
        when(mapper.list(any())).thenReturn(List.of(original));
        when(mapper.count(any())).thenReturn(1L);
        mvc.perform(get(BASE).param("managementItemCode", "EMPLOYMENT_RATE_ACHIEVEMENT")
                        .requestAttr("currentUser", user("R01", "R02", "R04")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].achievementId").value(10));
        org.mockito.ArgumentCaptor<Map<String, Object>> captor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper).list(captor.capture());
        verify(mapper).count(eq(captor.getValue()));
        assertThat(captor.getValue().get("roles")).isEqualTo(List.of("R01", "R02", "R04"));
    }

    @Test
    void ambiguousYearUnknownItemAndInvalidAttachmentAreRejected() throws Exception {
        when(mapper.years("DEPT")).thenReturn(List.of("2026", "2027"));
        mvc.perform(post(BASE).requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.evaluationYear").exists());
        verify(mapper, never()).insert(any());
    }

    @Test
    void unknownManagementItemIsRejected() throws Exception {
        when(mapper.managementItems(any())).thenReturn(List.of());
        mvc.perform(post(BASE).requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.managementItemCode").exists());
    }

    @Test
    void arbitraryAttachmentIsRejected() throws Exception {
        mvc.perform(post(BASE).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.trim().replace("}", ",\"attachmentIds\":[\"not-a-file\"]}")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.attachmentIds").exists());
        verify(mapper, never()).insert(any());
    }

    @Test
    void noRoleOrR07CannotCallIndividualOperations() throws Exception {
        for (CurrentUser principal : List.of(user("R07"), user("R03"))) {
            mvc.perform(get(BASE).requestAttr("currentUser", principal)).andExpect(status().isForbidden());
            mvc.perform(get(BASE + "/10").requestAttr("currentUser", principal)).andExpect(status().isForbidden());
            mvc.perform(post(BASE).requestAttr("currentUser", principal)
                    .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
            mvc.perform(put(BASE + "/10").requestAttr("currentUser", principal)
                    .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        }
    }

    @Test
    void readOnlyRoleCannotMutate() throws Exception {
        mvc.perform(post(BASE).requestAttr("currentUser", user("R02"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }

    @Test
    void downloadIsRealXlsxAndR07DoesNotCallList() throws Exception {
        when(mapper.list(any())).thenReturn(List.of(original));
        byte[] body = mvc.perform(get(BASE + "/download").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(content().contentType(EmploymentRateXlsxCodec.MIME))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(EmploymentRateXlsxCodec.read(body).get(1).get(0)).isEqualTo("ERA-test");
        mvc.perform(get(BASE + "/download").requestAttr("currentUser", user("R03")))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadRolesAndSuccessEnvelope() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "valid.xlsx", EmploymentRateXlsxCodec.MIME,
                EmploymentRateXlsxCodec.write(List.of(List.of("a"))));
        when(excel.upload(any(), any(), any())).thenReturn(new ExcelUploadResult(
                "up", "EMPLOYMENT_RATE_ACHIEVEMENT", "valid.xlsx", "COMMITTED", 1, 1, 0, 0, 1, List.of()));
        mvc.perform(multipart(BASE + "/excel-uploads").file(file).requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(1));
        mvc.perform(multipart(BASE + "/excel-uploads").file(file).requestAttr("currentUser", user("R01")))
                .andExpect(status().isForbidden());
    }

    @Test
    void bulkMissingYearIsFieldErrorAndR01IsDenied() throws Exception {
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", user("R07"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.evaluationYear").exists());
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"GENERATE\",\"evaluationYear\":\"2026\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bothUnapprovedBulkActionsAre409AndWriteNothing() throws Exception {
        for (String action : List.of("GENERATE", "DELETE")) {
            mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", user("R07"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"actionType\":\"" + action + "\",\"evaluationYear\":\"2026\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("BULK_POLICY_NOT_APPROVED"));
        }
        org.mockito.Mockito.verifyNoInteractions(mapper);
    }

    @Test
    void jobResultRequiresExecutorAndScope() throws Exception {
        when(mapper.job(7L)).thenReturn(Map.of("jobId", "7", "createdBy", 101L, "organizationCode", "DEPT",
                "totalCount", 1, "processedCount", 0, "unprocessedCount", 1));
        when(mapper.jobItems(7L)).thenReturn(List.of(Map.of("processedYn", "N", "unprocessedReason", "미처리")));
        when(mapper.departmentScope(101L, "DEPT")).thenReturn(1);
        mvc.perform(get(BASE + "/bulk-jobs/7").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].processedYn").value("N"));
        when(mapper.departmentScope(101L, "DEPT")).thenReturn(0);
        mvc.perform(get(BASE + "/bulk-jobs/7").requestAttr("currentUser", user("R07")))
                .andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/bulk-jobs/missing").requestAttr("currentUser", user("R07")))
                .andExpect(status().isNotFound());
    }

    @Test
    void administratorBypassIsKeptForReadAndWriteRoles() throws Exception {
        original.put("teacherUserId", 999L);
        mvc.perform(put(BASE + "/10").requestAttr("currentUser", user("R09"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk());
    }

    private static CurrentUser user(String... roles) {
        return new CurrentUser(101L, "teacher", "E101", "교원", List.of(roles), List.of());
    }

    private static Map<String, Object> row() {
        return new LinkedHashMap<>(Map.of(
                "achievementId", 10L, "managementNo", "ERA-test", "teacherUserId", 101L,
                "organizationCode", "DEPT", "evaluationYear", "2026", "managementItemCode", "EMPLOYMENT_RATE_ACHIEVEMENT",
                "achievementDate", LocalDate.parse("2026-04-01"), "achievementStatus", "DRAFT",
                "achievementDetail", "{\"achievementName\":\"원본\"}", "attachmentRef", "[]"));
    }
}
