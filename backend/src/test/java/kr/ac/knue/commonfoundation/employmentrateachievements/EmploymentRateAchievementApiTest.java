package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.common.educationachievements.EducationAchievementAccessPolicy;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** Real controller, services and access policy; only SQL and shared grant lookups are doubled. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EmploymentRateAchievementService.class, EmploymentRateExcelService.class,
        EducationAchievementAccessPolicy.class, EmploymentRateFileStorage.class})
class EmploymentRateAchievementApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EmploymentRateFileStorage storage;
    @MockBean EmploymentRateAchievementMapper mapper;
    @MockBean EducationAchievementGuardMapper guard;
    @MockBean EffectivePermissionService menu;
    @MockBean FunctionPermissionService functions;
    private final CurrentUser teacher = user(101L, "R01");
    private final CurrentUser operator = user(107L, "R07");
    private final CurrentUser administrator = user(1L, "R09");
    private final Map<Long, Map<String, Object>> stored = new LinkedHashMap<>();
    private final List<Map<String, Object>> changes = new ArrayList<>();
    private final List<String> files = new ArrayList<>();
    private static final String BODY = """
            {"evaluationYear":"2026","managementItemCode":"EMPLOYMENT_RATE_ACHIEVEMENT",
             "achievementDate":"2026-04-11","achievementName":"취업률","achievementDetail":{"notes":"상세"}}
            """;
    private static final List<String> COLUMNS = List.of("templateVersion", "employeeNo", "evaluationYear",
            "managementItemCode", "achievementDate", "achievementName", "achievementDetail", "attachmentRef");

    @BeforeEach
    void setUp() {
        stored.clear();
        changes.clear();
        when(menu.canAccess(anyLong(), anyList(), anyString())).thenReturn(true);
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(guard.countCertificationScope(anyLong(), anyLong())).thenReturn(1);
        when(mapper.organization(anyLong())).thenReturn("KNUE-DEPT-COMP");
        when(mapper.rules(anyString(), anyString())).thenReturn(List.of(
                Map.of("dataType", "TEXT", "requiredYn", "N", "teacherEditableYn", "Y")));
        when(mapper.find(anyLong(), anyBoolean())).thenAnswer(call -> {
            Map<String, Object> row = stored.get(call.getArgument(0));
            return row == null ? null : new LinkedHashMap<>(row);
        });
        when(mapper.insert(anyMap())).thenAnswer(call -> {
            Map<String, Object> row = new LinkedHashMap<>(call.getArgument(0));
            long id = 301L + stored.size();
            row.put("achievementId", id);
            row.put("achievementStatus", "DRAFT");
            stored.put(id, row);
            return id;
        });
        when(mapper.update(anyMap())).thenAnswer(call -> {
            Map<String, Object> row = call.getArgument(0);
            stored.get(((Number) row.get("achievementId")).longValue()).putAll(row);
            return 1;
        });
        doAnswer(call -> {
            changes.add(new LinkedHashMap<>(Map.of("field", call.getArgument(1),
                    "before", Objects.toString(call.getArgument(2), ""),
                    "after", Objects.toString(call.getArgument(3), ""))));
            return null;
        }).when(mapper).history(anyMap(), anyString(), nullable(String.class), nullable(String.class), anyString());
        when(mapper.list(anyMap())).thenAnswer(call -> new ArrayList<>(stored.values()));
        when(mapper.count(anyMap())).thenAnswer(call -> (long) stored.size());
        when(mapper.template()).thenReturn(Map.of("templateId", "EMPLOYMENT_RATE_ACHIEVEMENT", "templateVersion", "1.0"));
        when(mapper.templateColumns(anyString())).thenReturn(COLUMNS);
        when(mapper.employee("E0101")).thenReturn(101L);
        doAnswer(call -> {
            Map<String, Object> row = call.getArgument(0);
            files.add(row.get("fileToken").toString());
            if (row.get("errorFileToken") != null) files.add(row.get("errorFileToken").toString());
            return null;
        }).when(mapper).upload(anyMap());
    }

    @AfterEach
    void cleanup() {
        files.forEach(token -> storage.remove(token, "test"));
        files.clear();
    }

    @Test
    void createReadbackAndAuditUseActualWriteArguments() throws Exception {
        mvc.perform(post("/api/business/employment-rate-achievements").requestAttr("currentUser", teacher)
                        .header("X-Request-Id", "create-trace").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementId").value(301))
                .andExpect(jsonPath("$.meta.requestId").value("create-trace"));
        mvc.perform(get("/api/business/employment-rate-achievements/{achievementId}", 301)
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementName").value("취업률"));
        mvc.perform(get("/api/business/employment-rate-achievements").requestAttr("currentUser", teacher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].achievementName").value("취업률"));
        assertThat(changes).extracting(r -> r.get("field"))
                .contains("achievementDetail", "achievementDate", "managementItemCode", "achievementName");
        verify(mapper).statusHistory(argThat(r -> "create-trace".equals(r.get("requestId"))));
    }

    @Test
    void updateKeepsYearAndAuditsEveryChangedField() throws Exception {
        seed("DRAFT", 101L);
        String body = BODY.replace("2026\"", "2025\"");
        mvc.perform(put("/api/business/employment-rate-achievements/{achievementId}", 301)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"));
        mvc.perform(get("/api/business/employment-rate-achievements/{achievementId}", 301)
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementName").value("취업률"));
        assertThat(changes).anySatisfy(r -> {
            assertThat(r.get("field")).isEqualTo("achievementName");
            assertThat(r.get("before")).isEqualTo("원본");
            assertThat(r.get("after")).isEqualTo("취업률");
        });
        verify(guard).countActiveInputPeriods("2026", 101L);
    }

    @Test
    void outsideOccurredDateWarnsAndStillSaves() throws Exception {
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(0);
        mvc.perform(post("/api/business/employment-rate-achievements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("2026-04-11", "2025-12-31")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        assertThat(stored.get(301L).get("evaluationYear")).isEqualTo("2026");
    }

    @Test
    void confirmedRowUpdateKeepsOriginalAndNoAudit() throws Exception {
        seed("EVALUATION_CONFIRMED", 101L);
        mvc.perform(put("/api/business/employment-rate-achievements/{achievementId}", 301)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.message").value(
                        org.hamcrest.Matchers.containsString("CONFIRMED_DATA_LOCKED")));
        assertThat(stored.get(301L).get("achievementName")).isEqualTo("원본");
        verify(mapper, never()).update(anyMap());
        assertThat(changes).isEmpty();
    }

    @Test
    void targetFinalizationBlocksCreate() throws Exception {
        when(guard.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        mvc.perform(post("/api/business/employment-rate-achievements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isConflict());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void inactivePeriodBlocksUpdate() throws Exception {
        seed("DRAFT", 101L);
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(put("/api/business/employment-rate-achievements/{achievementId}", 301)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.message").value(
                        org.hamcrest.Matchers.containsString("PERIOD_NOT_ACTIVE")));
        assertThat(stored.get(301L).get("achievementName")).isEqualTo("원본");
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void submittedIsNotEditable() throws Exception {
        seed("SUBMITTED", 101L);
        mvc.perform(put("/api/business/employment-rate-achievements/{achievementId}", 301)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void otherTeacherUpdateForbidden() throws Exception {
        seed("DRAFT", 102L);
        mvc.perform(put("/api/business/employment-rate-achievements/{achievementId}", 301)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void detailEnforcesSameScopeAndMultiRoleUnion() throws Exception {
        seed("DRAFT", 102L);
        mvc.perform(get("/api/business/employment-rate-achievements/{achievementId}", 301)
                        .requestAttr("currentUser", teacher)).andExpect(status().isForbidden());
        mvc.perform(get("/api/business/employment-rate-achievements/{achievementId}", 301)
                        .requestAttr("currentUser", user(101L, "R01", "R04"))).andExpect(status().isOk());
    }

    @Test
    void missingDetailReturns404() throws Exception {
        mvc.perform(get("/api/business/employment-rate-achievements/{achievementId}", 999)
                .requestAttr("currentUser", teacher)).andExpect(status().isNotFound());
    }

    @Test
    void missingUpdateReturns404WithoutInsert() throws Exception {
        mvc.perform(put("/api/business/employment-rate-achievements/{achievementId}", 999)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void missingManagementItemReturnsFieldError() throws Exception {
        mvc.perform(post("/api/business/employment-rate-achievements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("EMPLOYMENT_RATE_ACHIEVEMENT", "")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode"));
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void malformedDateUpdateReturns400WithoutSqlLeak() throws Exception {
        mvc.perform(put("/api/business/employment-rate-achievements/{achievementId}", 301)
                        .requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("2026-04-11", "bad-date")))
                .andExpect(status().isBadRequest()).andExpect(content().string(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQLException"))));
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void duplicateCreateDoesNotUpsert() throws Exception {
        when(mapper.duplicates(anyMap())).thenReturn(1);
        mvc.perform(post("/api/business/employment-rate-achievements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isConflict());
        verify(mapper, never()).insert(anyMap());
        assertThat(changes).isEmpty();
    }

    @Test
    void menuDeniedEvenWhenRoleAdmitted() throws Exception {
        when(menu.canAccess(anyLong(), anyList(), anyString())).thenReturn(false);
        mvc.perform(get("/api/business/employment-rate-achievements").requestAttr("currentUser", teacher))
                .andExpect(status().isForbidden());
        verify(mapper, never()).list(anyMap());
    }

    @Test
    void functionDeniedEvenWhenMenuAdmitted() throws Exception {
        when(functions.evaluate(any())).thenThrow(new ForbiddenException());
        mvc.perform(post("/api/business/employment-rate-achievements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void scopedFilteredListAndCountReceiveIdenticalQuery() throws Exception {
        CurrentUser multiple = user(101L, "R01", "R04");
        mvc.perform(get("/api/business/employment-rate-achievements").requestAttr("currentUser", multiple)
                        .param("managementItemCode", "EMPLOYMENT_RATE_ACHIEVEMENT").param("evaluationYear", "2026"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        org.mockito.ArgumentCaptor<Map<String, Object>> captor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper).list(captor.capture());
        verify(mapper).count(captor.getValue());
        assertThat(captor.getValue().get("roles")).isEqualTo(List.of("R01", "R04"));
        assertThat(captor.getValue().get("managementItemCode")).isEqualTo("EMPLOYMENT_RATE_ACHIEVEMENT");
    }

    @Test
    void downloadIsRealWorkbookAndR07DoesNotGetListAdmission() throws Exception {
        seed("DRAFT", 101L);
        byte[] bytes = mvc.perform(get("/api/business/employment-rate-achievements/download")
                        .requestAttr("currentUser", operator)).andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(EmploymentRateWorkbook.read(bytes)).hasSize(2);
        mvc.perform(get("/api/business/employment-rate-achievements").requestAttr("currentUser", operator))
                .andExpect(status().isForbidden());
    }

    @Test
    void bulkUnapprovedHasNoDomainWrites() throws Exception {
        mvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs").requestAttr("currentUser", operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\",\"confirmed\":true}"))
                .andExpect(status().isConflict());
        verify(mapper, never()).insert(anyMap());
        assertThat(changes).isEmpty();
    }

    @Test
    void bulkMissingYearHasFieldError() throws Exception {
        mvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs").requestAttr("currentUser", operator)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("evaluationYear"));
    }

    @Test
    void bulkTeacherDenied() throws Exception {
        mvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void jobShowsStoredDiagnosticCountsAndItems() throws Exception {
        when(mapper.job("diagnostic")).thenReturn(new HashMap<>(Map.of("jobId", "diagnostic",
                "requesterUserId", 107L, "totalCount", 1, "processedCount", 0, "unprocessedCount", 1)));
        when(mapper.jobItems("diagnostic")).thenReturn(List.of(Map.of("targetUserId", 101L, "processedYn", "N")));
        mvc.perform(get("/api/business/employment-rate-achievements/bulk-jobs/{jobId}", "diagnostic")
                        .requestAttr("currentUser", operator)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unprocessedCount").value(1))
                .andExpect(jsonPath("$.data.items[0].processedYn").value("N"));
    }

    @Test
    void missingJobIs404() throws Exception {
        when(mapper.job("missing")).thenReturn(null);
        mvc.perform(get("/api/business/employment-rate-achievements/bulk-jobs/{jobId}", "missing")
                .requestAttr("currentUser", operator)).andExpect(status().isNotFound());
    }

    @Test
    void otherOperatorsJobIsForbidden() throws Exception {
        when(mapper.job("other")).thenReturn(Map.of("requesterUserId", 108L));
        mvc.perform(get("/api/business/employment-rate-achievements/bulk-jobs/{jobId}", "other")
                .requestAttr("currentUser", operator)).andExpect(status().isForbidden());
        verify(mapper, never()).jobItems(anyString());
    }

    @Test
    void normalUploadStagesOnlyAndCommitUsesTheActualStagedPayload() throws Exception {
        List<String> staged = new ArrayList<>();
        doAnswer(c -> { staged.add(c.getArgument(2)); return null; })
                .when(mapper).stage(anyString(), anyInt(), anyString(), anyString());
        String response = mvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(workbook(false)).requestAttr("currentUser", operator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.validationStatus").value("VALIDATED"))
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(response).path("data").path("uploadId").asText();
        assertThat(stored).isEmpty();
        verify(mapper).uploadHistory(anyMap());
        when(mapper.findUpload(id, true)).thenReturn(Map.of("uploaderUserId", 107L, "validationStatus", "VALIDATED"));
        when(mapper.staged(id)).thenReturn(staged);
        mvc.perform(post("/api/business/employment-rate-achievements/excel-uploads/{uploadId}/commit", id)
                        .requestAttr("currentUser", operator)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedCount").value(1));
        assertThat(stored.get(301L).get("teacherUserId")).isEqualTo(101L);
        assertThat(stored.get(301L).get("achievementName")).isEqualTo("Excel 실적");
        verify(mapper).committed(eq(id), eq(1), anyString());
    }

    @Test
    void invalidUploadKeepsHistoryAndActualErrorWorkbookWithZeroDomainWrites() throws Exception {
        String response = mvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(workbook(true)).requestAttr("currentUser", operator))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.errorCount").value(1))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).path("data").path("errorFileToken").asText();
        assertThat(EmploymentRateWorkbook.read(storage.read(token))).hasSize(2);
        verify(mapper).uploadHistory(anyMap());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void duplicateFileRowsAreRejectedRatherThanUpdated() throws Exception {
        List<List<String>> rows = EmploymentRateWorkbook.read(workbook(false).getBytes());
        rows.add(rows.get(1));
        mvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(new MockMultipartFile("file", "data.xlsx", "application/octet-stream",
                                EmploymentRateWorkbook.write(rows))).requestAttr("currentUser", operator))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.errorCount").value(1));
        verify(mapper, never()).update(anyMap());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void malformedRenamedWorkbookIsDiagnosedNotImported() throws Exception {
        mvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(new MockMultipartFile("file", "data.xlsx", "application/octet-stream", "CSV".getBytes()))
                        .requestAttr("currentUser", operator)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.uploadId").isNotEmpty());
        verify(mapper).uploadHistory(anyMap());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void uploadTeacherForbiddenBeforeStaging() throws Exception {
        mvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                .file(workbook(false)).requestAttr("currentUser", teacher)).andExpect(status().isForbidden());
        verify(mapper, never()).upload(anyMap());
    }

    @Test
    void lastCommitRowConflictPreventsFirstInsert() throws Exception {
        when(mapper.findUpload("upload", true)).thenReturn(Map.of("uploaderUserId", 107L, "validationStatus", "VALIDATED"));
        Map<String, String> payload = Map.of("templateVersion", "1.0", "employeeNo", "E0101", "evaluationYear", "2026",
                "managementItemCode", "EMPLOYMENT_RATE_ACHIEVEMENT", "achievementDate", "2026-05-01");
        Map<String, String> last = new HashMap<>(payload);
        last.put("achievementDate", "2026-05-02");
        when(mapper.staged("upload")).thenReturn(List.of(json.writeValueAsString(payload), json.writeValueAsString(last)));
        when(mapper.duplicates(anyMap())).thenReturn(0, 1);
        mvc.perform(post("/api/business/employment-rate-achievements/excel-uploads/{uploadId}/commit", "upload")
                .requestAttr("currentUser", operator)).andExpect(status().isConflict());
        verify(mapper, never()).insert(anyMap());
        verify(mapper, never()).committed(anyString(), anyInt(), anyString());
    }

    @Test
    void recommitReturns409() throws Exception {
        when(mapper.findUpload("done", true)).thenReturn(Map.of("uploaderUserId", 107L, "validationStatus", "COMMITTED"));
        mvc.perform(post("/api/business/employment-rate-achievements/excel-uploads/{uploadId}/commit", "done")
                .requestAttr("currentUser", operator)).andExpect(status().isConflict());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void administratorRemainsAdmittedButConfirmedLockStillApplies() throws Exception {
        mvc.perform(get("/api/business/employment-rate-achievements").requestAttr("currentUser", administrator))
                .andExpect(status().isOk());
        seed("EVALUATION_CONFIRMED", 102L);
        mvc.perform(put("/api/business/employment-rate-achievements/{achievementId}", 301)
                        .requestAttr("currentUser", administrator).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void anonymousRequestIs401() throws Exception {
        mvc.perform(get("/api/business/employment-rate-achievements")).andExpect(status().isUnauthorized());
        verify(mapper, never()).list(anyMap());
    }

    @Test
    void packagedContractRemainsAvailableOnClasspath() throws Exception {
        try (var input = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThat(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .contains("operationId: createEmploymentRateAchievement");
        }
    }

    private void seed(String status, Long owner) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("achievementId", 301L);
        row.put("teacherUserId", owner);
        row.put("evaluationYear", "2026");
        row.put("managementItemCode", "EMPLOYMENT_RATE_ACHIEVEMENT");
        row.put("achievementDate", LocalDate.parse("2026-04-10"));
        row.put("achievementName", "원본");
        row.put("achievementDetail", "{}");
        row.put("achievementStatus", status);
        stored.put(301L, row);
    }

    private MockMultipartFile workbook(boolean invalid) {
        return new MockMultipartFile("file", "employment.xlsx", "application/octet-stream",
                EmploymentRateWorkbook.write(List.of(COLUMNS, List.of("1.0", "E0101", "2026",
                        "EMPLOYMENT_RATE_ACHIEVEMENT", invalid ? "bad-date" : "2026-05-01",
                        "Excel 실적", "{}", ""))));
    }

    private static CurrentUser user(Long id, String... roles) {
        return new CurrentUser(id, "test-user", "E0101", "교원", List.of(roles), List.of());
    }
}
