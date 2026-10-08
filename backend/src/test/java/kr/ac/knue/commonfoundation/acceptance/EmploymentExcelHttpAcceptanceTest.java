package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.employmentrateachievements.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/** HTTP wizard using the real codec, Excel service and domain service; DB/file adapters remain mocks.
 * Transaction callbacks prove commit/rollback orchestration, not physical SQL rollback or durable files.
 */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({EmploymentRateApiAdvice.class, EmploymentRateXlsxCodec.class, EmploymentRateAchievementService.class,
        EmploymentRateExcelService.class, EmploymentExcelHttpAcceptanceTest.Transactions.class})
class EmploymentExcelHttpAcceptanceTest {
    private static final String PATH = "/api/business/employment-rate-achievements/excel-uploads";
    private static final List<String> COLUMNS = List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조");
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EmploymentRateXlsxCodec codec;
    @Autowired ExcelAcceptanceTransactions transactions;
    @MockBean EmploymentRateAchievementMapper mapper;
    @MockBean EducationAchievementGuardMapper guards;
    @MockBean EmploymentRateFileStoragePort storage;
    private final CurrentUser operator = new CurrentUser(
            107L, "operator", "E0107", "담당자", List.of("R07"), List.of());
    private final List<Map<String, Object>> staged = new ArrayList<>();
    private final List<Map<String, Object>> errors = new ArrayList<>();
    private final Map<String, Object> upload = new LinkedHashMap<>();

    @BeforeEach
    void setup() {
        staged.clear();
        errors.clear();
        upload.clear();
        transactions.commits = 0;
        transactions.rollbacks = 0;
        when(mapper.template()).thenReturn(Map.of("templateId", "employment-rate-achievement-v1"));
        when(mapper.columns(anyString())).thenReturn(COLUMNS);
        when(mapper.teacher("E0101")).thenReturn(Map.of("teacherUserId", 101L));
        when(mapper.organization(101L)).thenReturn("KNUE-DEPT-COMP");
        when(mapper.setting(anyMap())).thenReturn(Map.of("teacherEditablePart", "SELF_REPORT"));
        when(guards.countSharedActiveOrganization(107L, 101L)).thenReturn(1);
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(mapper.errors(anyString())).thenAnswer(call -> List.copyOf(errors));
        when(mapper.stages(anyString())).thenAnswer(call -> List.copyOf(staged));
        when(mapper.uploadInfo(anyString())).thenAnswer(call -> upload.isEmpty() ? null : Map.copyOf(upload));
        when(mapper.lockUpload(anyString())).thenAnswer(call -> upload.isEmpty() ? null : Map.copyOf(upload));
        doAnswer(call -> {
            upload.putAll(call.getArgument(0));
            upload.put("uploaderUserId", 107L);
            return null;
        }).when(mapper).upload(anyMap());
        doAnswer(call -> { staged.add(new LinkedHashMap<>(call.getArgument(0))); return null; })
                .when(mapper).stage(anyMap());
        doAnswer(call -> { errors.add(new LinkedHashMap<>(call.getArgument(0))); return null; })
                .when(mapper).error(anyMap());
        doAnswer(call -> {
            Map<String, Object> row = call.getArgument(0);
            row.put("achievementId", 42L);
            return null;
        }).when(mapper).insert(anyMap());
    }

    @Test
    void validWorkbookIsValidationOnlyUntilConfirmedCommitWritesEveryRowAndAudit() throws Exception {
        String id = validate(List.of(row("첫째"), row("둘째")), 0);
        verify(mapper, never()).insert(anyMap());
        mvc.perform(post(PATH + "/" + id + "/commit").requestAttr("currentUser", operator)
                        .header("X-Request-Id", "commit-trace"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("commit-trace"));
        verify(mapper, times(2)).insert(anyMap());
        verify(mapper, times(2)).statusHistory(argThat(p -> "commit-trace".equals(p.get("requestId"))));
        verify(mapper, times(2)).audit(argThat(p -> "commit-trace".equals(p.get("requestId"))));
        verify(mapper).committed(id);
        assertThat(transactions.commits).isEqualTo(2);
        assertThat(transactions.rollbacks).isZero();
        verify(storage, never()).delete(anyString());
    }

    @Test
    void duplicateFileRowsRejectWholeCommitAndPreserveErrorDiagnostics() throws Exception {
        String id = validate(List.of(row("同一"), row("同一")), 1);
        mvc.perform(post(PATH + "/" + id + "/commit").requestAttr("currentUser", operator))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("UPLOAD_NOT_COMMITTABLE"));
        verify(mapper, never()).insert(anyMap());
        verify(mapper, never()).update(anyMap());
        verify(mapper, never()).committed(anyString());
        mvc.perform(get(PATH + "/" + id + "/errors").requestAttr("currentUser", operator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].rowNumber").value(3));
    }

    @Test
    void duplicateDiscoveredOnLastCommitRowWritesNoEarlierRowAndRetainsFailure() throws Exception {
        String id = validate(List.of(row("먼저"), row("나중")), 0);
        when(mapper.duplicates(anyMap())).thenReturn(0L, 1L);
        mvc.perform(post(PATH + "/" + id + "/commit").requestAttr("currentUser", operator))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("DUPLICATE_DATA"));
        verify(mapper, never()).insert(anyMap());
        verify(mapper, never()).committed(anyString());
        assertThat(transactions.rollbacks).isEqualTo(1);
        assertThat(transactions.commits).isEqualTo(2);
        assertThat(errors).anyMatch(error -> "COMMIT_FAILED".equals(error.get("errorCode")));
        verify(storage, never()).delete(anyString());
    }

    @Test
    void writeFailureRollsBackDomainTransactionThenCommitsIndependentDiagnostic() throws Exception {
        String id = validate(List.of(row("한건")), 0);
        doThrow(new IllegalStateException("private SQL credential detail")).when(mapper).audit(anyMap());
        mvc.perform(post(PATH + "/" + id + "/commit").requestAttr("currentUser", operator))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("credential"))));
        assertThat(transactions.rollbacks).isEqualTo(1);
        assertThat(transactions.commits).isEqualTo(2);
        verify(mapper, never()).committed(anyString());
        assertThat(errors).anyMatch(error -> "COMMIT_FAILED".equals(error.get("errorCode")));
        verify(storage, never()).delete(anyString());
    }

    @Test
    void renamedCsvIs400AndNeverStoredOrStaged() throws Exception {
        mvc.perform(multipart(PATH).file(new MockMultipartFile(
                        "file", "renamed.xlsx", "application/octet-stream", "a,b,c".getBytes()))
                        .requestAttr("currentUser", operator))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(storage, mapper);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"formula", "external", "entity"})
    void forbiddenWorkbookContentIs400BeforeStorage(String kind) throws Exception {
        byte[] original = codec.write(List.of(COLUMNS, row("입력")));
        var bytes = new java.io.ByteArrayOutputStream();
        try (var input = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(original));
                var output = new java.util.zip.ZipOutputStream(bytes)) {
            java.util.zip.ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] content = input.readAllBytes();
                if (entry.getName().endsWith("sheet1.xml")) {
                    String xml = new String(content, java.nio.charset.StandardCharsets.UTF_8);
                    if (kind.equals("formula")) {
                        xml = xml.replace("</sheetData>", "<row><c r=\"A3\"><f>1+1</f><v>2</v></c></row></sheetData>");
                    } else if (kind.equals("entity")) {
                        xml = xml.replace("<worksheet",
                                "<!DOCTYPE worksheet [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><worksheet");
                    }
                    content = xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
                output.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));
                output.write(content);
                output.closeEntry();
            }
            if (kind.equals("external")) {
                output.putNextEntry(new java.util.zip.ZipEntry("xl/_rels/forbidden.xml.rels"));
                output.write(("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" TargetMode=\"External\" Target=\"https://invalid.example\"/>"
                        + "</Relationships>").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        mvc.perform(multipart(PATH).file(new MockMultipartFile(
                        "file", "forbidden.xlsx", "application/octet-stream", bytes.toByteArray()))
                        .requestAttr("currentUser", operator)).andExpect(status().isBadRequest());
        verifyNoInteractions(storage, mapper);
    }

    @Test
    void missingMultipartFileIs400AndDoesNotEnterService() throws Exception {
        mvc.perform(multipart(PATH).requestAttr("currentUser", operator)).andExpect(status().isBadRequest());
        verifyNoInteractions(storage, mapper);
    }

    @Test
    void storedUploadBelongsOnlyToUploaderIncludingBinaryErrorDownload() throws Exception {
        String id = validate(List.of(row("검증")), 0);
        var other = new CurrentUser(108L, "other", "E0108", "다른 담당자", List.of("R07"), List.of());
        mvc.perform(get(PATH + "/" + id + "/errors").requestAttr("currentUser", other))
                .andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/" + id + "/errors/download").requestAttr("currentUser", other))
                .andExpect(status().isForbidden());
        mvc.perform(post(PATH + "/" + id + "/commit").requestAttr("currentUser", other))
                .andExpect(status().isForbidden());
        verify(storage, never()).read(anyString());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void downloadedTemplateIsActualReopenableWorkbook() throws Exception {
        byte[] bytes = mvc.perform(get(PATH + "/template").requestAttr("currentUser", operator))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(codec.read(bytes)).containsExactly(COLUMNS);
    }

    private String validate(List<List<String>> rows, int expectedErrors) throws Exception {
        List<List<String>> workbook = new ArrayList<>();
        workbook.add(COLUMNS);
        workbook.addAll(rows);
        String response = mvc.perform(multipart(PATH).file(new MockMultipartFile(
                        "file", "rates.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, codec.write(workbook)))
                        .requestAttr("currentUser", operator).header("X-Request-Id", "validation-trace"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(0))
                .andExpect(jsonPath("$.data.errorCount").value(expectedErrors))
                .andExpect(jsonPath("$.meta.requestId").value("validation-trace"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("uploadId").asText();
    }

    private List<String> row(String name) {
        return List.of("E0101", "EMPLOYMENT_RATE_ACHIEVEMENT", "2026-04-10", name, "");
    }

    @TestConfiguration
    static class Transactions {
        @Bean
        ExcelAcceptanceTransactions transactionManager() {
            return new ExcelAcceptanceTransactions();
        }
    }

    /** Same Spring synchronization-driving manager as the repository's existing Excel unit tests. */
    static class ExcelAcceptanceTransactions extends AbstractPlatformTransactionManager {
        int commits;
        int rollbacks;
        protected Object doGetTransaction() { return new Object(); }
        protected void doBegin(Object transaction, TransactionDefinition definition) { }
        protected void doCommit(DefaultTransactionStatus status) { commits++; }
        protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
