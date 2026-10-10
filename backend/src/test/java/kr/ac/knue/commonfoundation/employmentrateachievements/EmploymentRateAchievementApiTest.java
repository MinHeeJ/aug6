package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.excel.FileStoragePort;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

/** Real controller/service contract tests; the mapper double retains actual writes for HTTP readback. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EmploymentRateErrorAdvice.class,
        EmploymentRateAchievementService.class, EmploymentRateAchievementApiTest.Transactions.class})
class EmploymentRateAchievementApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean EmploymentRateAchievementMapper mapper;
    @MockBean EducationAchievementGuardMapper guards;
    @MockBean FileStoragePort files;
    private final Map<Long, Map<String, Object>> ledger = new HashMap<>();
    private final Map<String, Map<String, Object>> uploads = new HashMap<>();
    private final List<Map<String, Object>> staged = new ArrayList<>();
    private static final String ROOT = "/api/business/employment-rate-achievements";
    private static final List<String> COLUMNS = List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조");
    private long nextId;

    @TestConfiguration
    static class Transactions {
        @Bean
        PlatformTransactionManager manager() {
            return new AbstractPlatformTransactionManager() {
                protected Object doGetTransaction() { return new Object(); }
                protected void doBegin(Object transaction, TransactionDefinition definition) { }
                protected void doCommit(DefaultTransactionStatus status) { }
                protected void doRollback(DefaultTransactionStatus status) { }
            };
        }
    }

    private CurrentUser user(String... roles) {
        return new CurrentUser(101L, "teacher", "E101", "교원", List.of(roles), List.of());
    }

    @BeforeEach
    void setup() {
        ledger.clear(); uploads.clear(); staged.clear(); nextId = 300;
        when(mapper.function(anyMap())).thenReturn(1);
        when(mapper.managementItem(anyMap())).thenReturn(1);
        when(mapper.scope(anyMap())).thenReturn(1);
        when(mapper.organization(anyLong())).thenReturn("KNUE-DEPT-COMP");
        when(mapper.teacher(anyString())).thenReturn(101L);
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guards.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.find(anyMap())).thenAnswer(call -> {
            Map<String, Object> query = call.getArgument(0);
            var row = ledger.get(Long.valueOf(query.get("achievementId").toString()));
            return row == null ? null : new HashMap<>(row);
        });
        doAnswer(call -> {
            Map<String, Object> row = call.getArgument(0);
            row.put("achievementId", ++nextId);
            var saved = new HashMap<>(row);
            saved.put("achievementStatus", "DRAFT");
            ledger.put(nextId, saved);
            return null;
        }).when(mapper).insert(anyMap());
        when(mapper.update(anyMap())).thenAnswer(call -> {
            Map<String, Object> row = call.getArgument(0);
            var saved = ledger.get(Long.valueOf(row.get("achievementId").toString()));
            saved.putAll(row);
            return 1;
        });
        when(mapper.list(anyMap())).thenAnswer(call -> new ArrayList<>(ledger.values()));
        when(mapper.count(anyMap())).thenAnswer(call -> (long) ledger.size());
        when(mapper.managementItems()).thenReturn(List.of());
        when(mapper.template()).thenReturn("template");
        when(mapper.templateColumns(anyString())).thenReturn(COLUMNS);
        when(files.save(any())).thenReturn("retained-token");
        doAnswer(call -> {
            Map<String, Object> upload = new HashMap<>(call.getArgument(0));
            upload.put("uploaderUserId", upload.get("actorId"));
            uploads.put(upload.get("uploadId").toString(), upload);
            return null;
        }).when(mapper).upload(anyMap());
        doAnswer(call -> { staged.add(new HashMap<>(call.getArgument(0))); return null; })
                .when(mapper).staging(anyMap());
        when(mapper.findUpload(anyMap())).thenAnswer(call -> {
            Map<String, Object> query = call.getArgument(0);
            return uploads.get(query.get("uploadId"));
        });
        when(mapper.stagingRows(anyString())).thenAnswer(call -> new ArrayList<>(staged));
        doAnswer(call -> {
            uploads.get(call.getArgument(0).toString()).put("validationStatus", "COMMITTED"); return null;
        }).when(mapper).committed(anyString());
    }

    private String body(String title) throws Exception {
        return json.writeValueAsString(Map.of("managementItemCode", "EMPLOYMENT_RATE_ACHIEVEMENT",
                "achievementDate", "2026-04-10", "title", title));
    }

    private Long create() throws Exception {
        mvc.perform(post(ROOT).requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).header("X-Request-Id", "trace-create")
                        .content(body("기존 실적")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.requestId").value("trace-create"));
        return nextId;
    }

    @Test
    void createListDetailAndAuditUseActualWriteArguments() throws Exception {
        Long id = create();
        mvc.perform(get(ROOT).requestAttr("currentUser", user("R01")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].title").value("기존 실적"));
        mvc.perform(get(ROOT + "/{achievementId}", id).requestAttr("currentUser", user("R01")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("기존 실적"));
        verify(mapper).history(argThat(h -> h.get("before") == null
                && h.get("after").toString().contains("기존 실적") && "trace-create".equals(h.get("requestId"))));
        assertThat(new ClassPathResource("contracts/openapi.yaml").getInputStream().readAllBytes())
                .asString(StandardCharsets.UTF_8).contains("operationId: createEmploymentRateAchievement");
    }

    @Test
    void updateRetainsYearAndCapturesCompleteBeforeAfterSnapshot() throws Exception {
        Long id = create();
        String changed = json.writeValueAsString(Map.of("managementItemCode", "EMPLOYMENT_RATE_ACHIEVEMENT",
                "achievementDate", "2025-12-31", "evaluationYear", "2025", "title", "새 실적"));
        when(guards.countEvaluationDatePeriods(eq("2026"), anyLong(), any())).thenReturn(0);
        mvc.perform(put(ROOT + "/{achievementId}", id).requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(changed))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        mvc.perform(get(ROOT + "/{achievementId}", id).requestAttr("currentUser", user("R01")))
                .andExpect(jsonPath("$.data.title").value("새 실적"));
        verify(mapper).history(argThat(h -> "UPDATE".equals(h.get("changeType"))
                && h.get("before").toString().contains("기존 실적") && h.get("after").toString().contains("새 실적")));
    }

    @Test
    void missingManagementItemIsFieldError() throws Exception {
        mvc.perform(post(ROOT).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"실적\",\"achievementDate\":\"2026-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void invalidUpdateDateDoesNotWrite() throws Exception {
        mvc.perform(put(ROOT + "/{achievementId}", 301).requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"X\",\"title\":\"실적\",\"achievementDate\":\"bad\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void wrongRoleCannotCreate() throws Exception {
        mvc.perform(post(ROOT).requestAttr("currentUser", user("R07")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("실적"))).andExpect(status().isForbidden());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void othersCannotUpdateEvenWithReadScope() throws Exception {
        Long id = create(); ledger.get(id).put("teacherUserId", 202L);
        mvc.perform(put(ROOT + "/{achievementId}", id).requestAttr("currentUser", user("R01", "R02"))
                        .contentType(MediaType.APPLICATION_JSON).content(body("변경")))
                .andExpect(status().isForbidden());
        verify(mapper, never()).update(anyMap());
        assertThat(ledger.get(id).get("title")).isEqualTo("기존 실적");
    }

    @Test
    void confirmedRowIsUnchanged() throws Exception {
        Long id = create(); ledger.get(id).put("achievementStatus", "EVALUATION_CONFIRMED");
        mvc.perform(put(ROOT + "/{achievementId}", id).requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(body("변경")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        verify(mapper, never()).update(anyMap());
        assertThat(ledger.get(id).get("title")).isEqualTo("기존 실적");
    }

    @Test
    void finalizationBlocksCreateBeforeAnyWrite() throws Exception {
        when(guards.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        mvc.perform(post(ROOT).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("실적"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void inactivePeriodKeepsOriginalValue() throws Exception {
        Long id = create(); when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(put(ROOT + "/{achievementId}", id).requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(body("변경")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        assertThat(ledger.get(id).get("title")).isEqualTo("기존 실적");
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void stateCannotBeForcedToCertified() throws Exception {
        Long id = create();
        String body = body("변경").replace("}", ",\"achievementStatus\":\"CERTIFIED\"}");
        mvc.perform(put(ROOT + "/{achievementId}", id).requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void scopedDetailRefusesForbiddenOwner() throws Exception {
        Long id = create(); when(mapper.scope(anyMap())).thenReturn(0);
        mvc.perform(get(ROOT + "/{achievementId}", id).requestAttr("currentUser", user("R02")))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailMissingIs404() throws Exception {
        mvc.perform(get(ROOT + "/{achievementId}", 999).requestAttr("currentUser", user("R01")))
                .andExpect(status().isNotFound());
    }

    @Test
    void filteredListAndCountReceiveIdenticalUnionScope() throws Exception {
        mvc.perform(get(ROOT).requestAttr("currentUser", user("R01", "R02", "R04"))
                        .param("managementNo", "ERA-selected").param("pageSize", "50"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        var list = org.mockito.ArgumentCaptor.forClass(Map.class);
        var count = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper).list(list.capture()); verify(mapper).count(count.capture());
        assertThat(list.getValue()).isEqualTo(count.getValue());
        assertThat(list.getValue().get("roles")).isEqualTo(List.of("R01", "R02", "R04"));
    }

    @Test
    void listWrongRoleIs403() throws Exception {
        mvc.perform(get(ROOT).requestAttr("currentUser", user("R07"))).andExpect(status().isForbidden());
    }

    @Test
    void badPageSizeIs400() throws Exception {
        mvc.perform(get(ROOT).requestAttr("currentUser", user("R01")).param("pageSize", "13"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void downloadIsRealWorkbookForOperator() throws Exception {
        byte[] bytes = mvc.perform(get(ROOT + "/download").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(content().contentType(EmploymentRateWorkbook.CONTENT_TYPE))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(EmploymentRateWorkbook.read(bytes).get(0)).isEqualTo(COLUMNS);
    }

    @Test
    void downloadWrongRoleIs403() throws Exception {
        mvc.perform(get(ROOT + "/download").requestAttr("currentUser", user("R03")))
                .andExpect(status().isForbidden());
    }

    @Test
    void bulkUnapprovedIs409AndCreatesNothing() throws Exception {
        mvc.perform(post(ROOT + "/bulk-jobs").requestAttr("currentUser", user("R07"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\",\"targetConditionJson\":{}}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("BULK_POLICY_NOT_APPROVED"));
        verify(mapper, never()).insert(anyMap());
        verify(mapper, never()).job(anyString());
    }

    @Test
    void bulkMissingYearIs400() throws Exception {
        mvc.perform(post(ROOT + "/bulk-jobs").requestAttr("currentUser", user("R07"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"GENERATE\",\"targetConditionJson\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'evaluationYear')]").isNotEmpty());
    }

    @Test
    void facultyCannotRunBulk() throws Exception {
        mvc.perform(post(ROOT + "/bulk-jobs").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"actionType\":\"DELETE\",\"targetConditionJson\":{}}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void jobReadsPersistedItemsForOwner() throws Exception {
        when(mapper.job("result")).thenReturn(Map.of("jobId", "result", "requesterUserId", 101L, "totalCount", 1));
        when(mapper.jobItems("result")).thenReturn(List.of(Map.of("processedYn", "N", "unprocessedReason", "미처리")));
        mvc.perform(get(ROOT + "/bulk-jobs/{jobId}", "result").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].processedYn").value("N"));
    }

    @Test
    void jobMissingIs404() throws Exception {
        when(mapper.job("absent")).thenReturn(null);
        mvc.perform(get(ROOT + "/bulk-jobs/{jobId}", "absent").requestAttr("currentUser", user("R07")))
                .andExpect(status().isNotFound());
    }

    @Test
    void jobOtherOwnerIs403() throws Exception {
        when(mapper.job("other")).thenReturn(Map.of("requesterUserId", 202L));
        mvc.perform(get(ROOT + "/bulk-jobs/{jobId}", "other").requestAttr("currentUser", user("R07")))
                .andExpect(status().isForbidden());
    }

    private MockMultipartFile file(List<List<String>> rows) {
        return new MockMultipartFile("file", "실적.xlsx", EmploymentRateWorkbook.CONTENT_TYPE,
                EmploymentRateWorkbook.write(rows));
    }

    private String validatedUpload() throws Exception {
        String result = mvc.perform(multipart(ROOT + "/excel-uploads").file(file(List.of(COLUMNS,
                        List.of("E101", "EMPLOYMENT_RATE_ACHIEVEMENT", "2026-04-10", "파일 실적", ""))))
                        .requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(0))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(result).path("data").path("uploadId").asText();
    }

    @Test
    void validUploadStagesThenExplicitCommitMaterializesAllRowsAndHistory() throws Exception {
        String id = validatedUpload(); verify(mapper, never()).insert(anyMap());
        mvc.perform(post(ROOT + "/excel-uploads/{uploadId}/commit", id).requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(1));
        assertThat(ledger.values().iterator().next().get("teacherUserId")).isEqualTo(101L);
        verify(mapper).uploadHistory(anyMap()); verify(mapper).history(anyMap());
        mvc.perform(post(ROOT + "/excel-uploads/{uploadId}/commit", id).requestAttr("currentUser", user("R07")))
                .andExpect(status().isConflict());
        verify(mapper, times(1)).insert(anyMap());
    }

    @Test
    void oneInvalidRowRetainsEvidenceAndNoBusinessWrite() throws Exception {
        mvc.perform(multipart(ROOT + "/excel-uploads").file(file(List.of(COLUMNS,
                        List.of("E101", "EMPLOYMENT_RATE_ACHIEVEMENT", "2026-04-10", "正常", ""),
                        List.of("E101", "EMPLOYMENT_RATE_ACHIEVEMENT", "2026-04-10", "", ""))))
                        .requestAttr("currentUser", user("R07")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.meta.uploadId").isNotEmpty())
                .andExpect(jsonPath("$.meta.errorDownloadUrl").isNotEmpty());
        verify(mapper, never()).insert(anyMap()); verify(mapper).error(anyMap());
        verify(mapper).uploadHistory(argThat(h -> Integer.valueOf(1).equals(h.get("errorCount"))));
    }

    @Test
    void duplicateWorkbookDoesNotUpdateExistingValues() throws Exception {
        var row = List.of("E101", "EMPLOYMENT_RATE_ACHIEVEMENT", "2026-04-10", "重複", "");
        mvc.perform(multipart(ROOT + "/excel-uploads").file(file(List.of(COLUMNS, row, row)))
                        .requestAttr("currentUser", user("R07")))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insert(anyMap()); verify(mapper, never()).update(anyMap());
    }

    @Test
    void databaseDuplicateRetainsOriginal() throws Exception {
        when(mapper.duplicate(anyMap())).thenReturn(1);
        mvc.perform(multipart(ROOT + "/excel-uploads").file(file(List.of(COLUMNS,
                        List.of("E101", "EMPLOYMENT_RATE_ACHIEVEMENT", "2026-04-10", "중복", ""))))
                        .requestAttr("currentUser", user("R07")))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).insert(anyMap()); verify(mapper, never()).update(anyMap());
    }

    @Test
    void malformedWorkbookRetainsDiagnostics() throws Exception {
        var file = new MockMultipartFile("file", "bad.xlsx", EmploymentRateWorkbook.CONTENT_TYPE,
                "not a workbook".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart(ROOT + "/excel-uploads").file(file).requestAttr("currentUser", user("R07")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.meta.uploadId").isNotEmpty());
        verify(mapper).error(anyMap()); verify(mapper, never()).insert(anyMap());
    }

    @Test
    void uploadWrongRoleIs403() throws Exception {
        mvc.perform(multipart(ROOT + "/excel-uploads").file(file(List.of(COLUMNS)))
                        .requestAttr("currentUser", user("R01"))).andExpect(status().isForbidden());
        verify(mapper, never()).upload(anyMap());
    }

    @Test
    void commitRevalidatesLastRowBeforeAnyWrite() throws Exception {
        String id = validatedUpload(); staged.add(new HashMap<>(staged.get(0)));
        when(mapper.duplicate(anyMap())).thenReturn(0, 1);
        mvc.perform(post(ROOT + "/excel-uploads/{uploadId}/commit", id).requestAttr("currentUser", user("R07")))
                .andExpect(status().isConflict());
        verify(mapper, never()).insert(anyMap()); verify(mapper, never()).committed(anyString());
    }

    @Test
    void otherUploadOwnerCannotCommit() throws Exception {
        String id = validatedUpload(); uploads.get(id).put("uploaderUserId", 202L);
        mvc.perform(post(ROOT + "/excel-uploads/{uploadId}/commit", id).requestAttr("currentUser", user("R07")))
                .andExpect(status().isForbidden());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void templateHistoriesAndErrorDownloadUseOwnedEndpoints() throws Exception {
        mvc.perform(get(ROOT + "/excel-uploads/template").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(content().contentType(EmploymentRateWorkbook.CONTENT_TYPE));
        mvc.perform(get(ROOT + "/excel-uploads/histories").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk());
        String id = validatedUpload();
        mvc.perform(get(ROOT + "/excel-uploads/{uploadId}/errors", id).requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk());
        mvc.perform(get(ROOT + "/excel-uploads/{uploadId}/errors/download", id)
                        .requestAttr("currentUser", user("R07"))).andExpect(status().isOk())
                .andExpect(content().contentType(EmploymentRateWorkbook.CONTENT_TYPE));
    }

    @Test
    void administratorBypassRemainsAllowed() throws Exception {
        mvc.perform(get(ROOT).requestAttr("currentUser", user("R09"))).andExpect(status().isOk());
        mvc.perform(get(ROOT + "/download").requestAttr("currentUser", user("R09"))).andExpect(status().isOk());
    }
}
