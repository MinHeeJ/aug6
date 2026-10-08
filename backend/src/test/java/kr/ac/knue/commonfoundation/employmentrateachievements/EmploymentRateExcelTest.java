package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class EmploymentRateExcelTest {
    private EmploymentRateAchievementMapper mapper;
    private EmploymentRateFileStoragePort storage;
    private EmploymentRateExcelService excel;
    private EmploymentRateXlsxCodec codec;
    private CurrentUser operator;
    private final List<String> columns = List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조");
    private final List<Map<String, Object>> stages = new ArrayList<>();

    @BeforeEach
    void setup() {
        mapper = mock(EmploymentRateAchievementMapper.class);
        EducationAchievementGuardMapper guards = mock(EducationAchievementGuardMapper.class);
        storage = mock(EmploymentRateFileStoragePort.class);
        codec = new EmploymentRateXlsxCodec();
        ObjectMapper json = new ObjectMapper().findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var service = new EmploymentRateAchievementService(mapper, guards, json);
        excel = new EmploymentRateExcelService(mapper, service, codec, storage, json, new TestTransactions());
        operator = new CurrentUser(101L, "operator", "E0101", "담당자", List.of("R07"), List.of());
        when(mapper.template()).thenReturn(Map.of("templateId", "employment-rate-achievement-v1"));
        when(mapper.columns(anyString())).thenReturn(columns);
        when(mapper.teacher("E0101")).thenReturn(Map.of("teacherUserId", 101L));
        when(mapper.organization(101L)).thenReturn("KNUE-DEPT-COMP");
        when(mapper.setting(any())).thenReturn(Map.of("teacherEditablePart", "SELF_REPORT"));
        when(guards.countSharedActiveOrganization(101L, 101L)).thenReturn(1);
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(mapper.errors(anyString())).thenReturn(List.of());
        doAnswer(call -> {
            stages.add(new LinkedHashMap<>(call.getArgument(0)));
            return null;
        }).when(mapper).stage(any());
        doAnswer(call -> {
            Map<String, Object> p = call.getArgument(0);
            p.put("achievementId", 42L);
            return null;
        }).when(mapper).insert(any());
        when(mapper.uploadInfo(anyString())).thenReturn(Map.of("uploaderUserId", 101L, "validationStatus", "VALIDATED"));
        when(mapper.lockUpload(anyString())).thenReturn(Map.of("uploaderUserId", 101L, "validationStatus", "VALIDATED"));
        when(mapper.stages(anyString())).thenAnswer(call -> stages);
    }

    private MockMultipartFile file(List<List<String>> data) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(columns);
        rows.addAll(data);
        return new MockMultipartFile("file", "rates.xlsx", "application/octet-stream", codec.write(rows));
    }

    private List<String> row(String name) {
        return List.of("E0101", "EMPLOYMENT_RATE_ACHIEVEMENT", "2026-04-10", name, "");
    }

    @Test
    void validUploadStagesThenRealCommitWritesAllHeadersAndAudit() {
        Map<String, Object> result = excel.upload(file(List.of(row("first"), row("second"))), operator, "trace");
        assertThat(result.get("errorCount")).isEqualTo(0);
        assertThat(result.get("savedCount")).isEqualTo(0);
        verify(mapper, never()).insert(any());
        Map<String, Object> committed = excel.commit((String) result.get("uploadId"), operator, "trace-commit");
        assertThat(committed.get("savedCount")).isEqualTo(2);
        verify(mapper, times(2)).insert(any());
        verify(mapper, times(2)).audit(argThat(p -> "trace-commit".equals(p.get("requestId"))));
        verify(mapper).committed(result.get("uploadId").toString());
        verify(storage, times(2)).put(anyString(), any());
        verify(storage, never()).delete(anyString());
    }

    @Test
    void oneErrorPreservesOriginalDiagnosticsHistoryAndZeroDomainWrites() {
        Map<String, Object> result = excel.upload(file(List.of(row("good"),
                List.of("unknown", "EMPLOYMENT_RATE_ACHIEVEMENT", "2026-04-10", "bad", ""))), operator, "trace");
        assertThat(result.get("errorCount")).isEqualTo(1);
        verify(mapper).error(argThat(p -> Integer.valueOf(3).equals(p.get("rowNumber"))));
        verify(mapper).history(argThat(p -> Integer.valueOf(0).equals(p.get("savedCount"))));
        verify(storage, times(2)).put(anyString(), any());
        verify(mapper, never()).insert(any());
    }

    @Test
    void duplicateRowsAreErrorsNotUpserts() {
        Map<String, Object> result = excel.upload(file(List.of(row("same"), row("same"))), operator, "trace");
        assertThat(result.get("errorCount")).isEqualTo(1);
        verify(mapper, never()).insert(any());
        verify(mapper, never()).update(any());
    }

    @Test
    void existingDuplicateDoesNotChangeBusinessData() {
        when(mapper.duplicates(any())).thenReturn(1L);
        assertThat(excel.upload(file(List.of(row("same"))), operator, "trace").get("errorCount")).isEqualTo(1);
        verify(mapper, never()).insert(any());
        verify(mapper, never()).update(any());
    }

    @Test
    void lastRowCommitRevalidationConflictWritesNoEarlierValidRow() {
        Map<String, Object> result = excel.upload(file(List.of(row("first"), row("second"))), operator, "trace");
        when(mapper.duplicates(any())).thenReturn(0L, 1L);
        assertThatThrownBy(() -> excel.commit(result.get("uploadId").toString(), operator, "trace"))
                .hasMessageContaining("DUPLICATE_DATA");
        verify(mapper, never()).insert(any());
        verify(mapper, never()).committed(any());
        verify(mapper).error(argThat(p -> "COMMIT_FAILED".equals(p.get("errorCode"))));
    }

    @Test
    void recordedErrorPreventsCommit() {
        when(mapper.errors(anyString())).thenReturn(List.of(Map.of("rowNumber", 2)));
        assertThatThrownBy(() -> excel.commit("selected", operator, "trace"))
                .hasMessageContaining("UPLOAD_NOT_COMMITTABLE");
        verify(mapper, never()).insert(any());
    }

    @Test
    void otherUploaderCannotReadOrCommit() {
        when(mapper.uploadInfo(anyString())).thenReturn(Map.of("uploaderUserId", 102L));
        assertThatThrownBy(() -> excel.errors("selected", operator))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ForbiddenException.class);
        assertThatThrownBy(() -> excel.commit("selected", operator, "trace"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ForbiddenException.class);
    }

    @Test
    void failedDiagnosticTransactionDeletesOnlyNewFiles() {
        doThrow(new RuntimeException("DB failure")).when(mapper).upload(any());
        assertThatThrownBy(() -> excel.upload(file(List.of(row("new"))), operator, "trace"))
                .isInstanceOf(RuntimeException.class);
        verify(storage, times(2)).delete(anyString());
        verify(mapper, never()).insert(any());
    }

    @Test
    void malformedOrRenamedCsvIsRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "renamed.xlsx", "application/octet-stream", "a,b".getBytes());
        assertThatThrownBy(() -> excel.upload(file, operator, "trace")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void templateAndStringInjectionRoundTripAsRealXlsx() {
        assertThat(codec.read(excel.template(operator))).containsExactly(columns);
        assertThat(codec.read(codec.write(List.of(List.of("=HYPERLINK(\"bad\")", "<&")))))
                .containsExactly(List.of("=HYPERLINK(\"bad\")", "<&"));
    }

    @Test
    void formulasAndExternalEntitiesAreRejected() throws Exception {
        assertThatThrownBy(() -> codec.read(archive("<row><c r=\"A1\"><f>1+1</f><v>2</v></c></row>")))
                .isInstanceOf(IllegalArgumentException.class);
        byte[] malicious = archive("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><row>&e;</row>");
        assertThatThrownBy(() -> codec.read(malicious)).isInstanceOf(IllegalArgumentException.class);
    }

    private byte[] archive(String rows) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
            for (String name : List.of("[Content_Types].xml", "xl/workbook.xml", "xl/worksheets/sheet1.xml")) {
                zip.putNextEntry(new java.util.zip.ZipEntry(name));
                String xml = name.endsWith("sheet1.xml")
                        ? "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                                + rows + "</worksheet>" : "<root/>";
                zip.write(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    /** Drives Spring synchronization lifecycle without a live DB; SQL atomicity remains a runner check. */
    static class TestTransactions extends AbstractPlatformTransactionManager {
        protected Object doGetTransaction() { return new Object(); }
        protected void doBegin(Object transaction, TransactionDefinition definition) { }
        protected void doCommit(DefaultTransactionStatus status) { }
        protected void doRollback(DefaultTransactionStatus status) { }
    }
}
