package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.storage.FileStoragePort;
import kr.ac.knue.commonfoundation.common.storage.FileStorageTransactionSupport;
import kr.ac.knue.commonfoundation.common.storage.LocalFileStorageAdapter;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/** Behavior tests cross real business logic and file synchronization without requiring an external database. */
class EmploymentRateAchievementServiceTest {
    @TempDir
    Path directory;
    private EmploymentRateAchievementMapper mapper;
    private EducationAchievementGuardMapper guard;
    private FunctionPermissionService permissions;
    private FileStoragePort storage;
    private EmploymentRateAchievementService service;
    private ObjectMapper json;
    private final CurrentUser faculty = user("R01");

    @BeforeEach
    void setup() {
        mapper = mock(EmploymentRateAchievementMapper.class);
        guard = mock(EducationAchievementGuardMapper.class);
        permissions = mock(FunctionPermissionService.class);
        storage = new LocalFileStorageAdapter(directory.toString(), 10485760);
        json = new ObjectMapper().findAndRegisterModules();
        service = new EmploymentRateAchievementService(mapper, guard, permissions, storage,
                new FileStorageTransactionSupport(storage), json, new SynchronizingTransactionManager());
        when(mapper.organizations(anyLong())).thenReturn(List.of("department"));
        when(mapper.managementItemCount(any())).thenReturn(1);
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
    }

    @Test
    void createUsesGeneratedKeyBeforeReadAndWritesFullSnapshotAndStatus() {
        doAnswer(call -> {
            Map<String, Object> row = call.getArgument(0);
            row.put("achievementId", 88L);
            return null;
        }).when(mapper).insert(any());
        when(mapper.find(any())).thenReturn(row("DRAFT", "first", "2026-04-10"));
        Map<String, Object> result = service.create(request("first", "2026-04-10"), faculty, "request-create");
        assertThat(((Map<?, ?>) result.get("achievement")).get("achievementId")).isEqualTo(88L);
        var order = inOrder(mapper);
        order.verify(mapper).organizations(101L);
        order.verify(mapper).managementItemCount(any());
        order.verify(mapper).duplicate(any());
        order.verify(mapper).insert(any());
        order.verify(mapper).initialStatus(argThat(row -> row.get("achievementId").equals(88L)));
        order.verify(mapper).find(argThat(row -> row.get("achievementId").equals(88L)));
        verify(mapper).history(argThat(row -> row.get("afterValue").toString().contains("first")
                && row.get("beforeValue") == null && row.get("requestId").equals("request-create")));
    }

    @Test
    void updatePreservesStoredYearAndAuditsEveryChangedFieldAndAttachment() throws Exception {
        Map<String, Object> old = row("DRAFT", "old", "2026-04-10");
        when(mapper.lock(any())).thenReturn(old);
        when(mapper.update(any())).thenReturn(1);
        Map<String, Object> updated = row("DRAFT", "new", "2025-12-31");
        when(mapper.find(any())).thenReturn(updated);
        when(guard.countEvaluationDatePeriods(eq("2026"), eq(101L), any())).thenReturn(0);
        Map<String, Object> result = service.update(88L, request("new", "2025-12-31"), faculty, "request-update");
        assertThat(result.get("occurredDateWarning")).isEqualTo(true);
        verify(mapper).update(argThat(row -> "2026".equals(row.get("evaluationYear"))
                && LocalDate.parse("2025-12-31").equals(row.get("achievementDate"))));
        ArgumentCaptor<Map<String, Object>> history = ArgumentCaptor.forClass(Map.class);
        verify(mapper).history(history.capture());
        Map<String, Object> before = json.readValue(history.getValue().get("beforeValue").toString(), Map.class);
        Map<String, Object> after = json.readValue(history.getValue().get("afterValue").toString(), Map.class);
        assertThat(before).containsEntry("achievementName", "old").containsEntry("achievementDate", "2026-04-10");
        assertThat(after).containsEntry("achievementName", "new").containsEntry("achievementDate", "2025-12-31")
                .containsEntry("evaluationYear", "2026").containsEntry("attachmentIds", List.of());
    }

    @Test
    void confirmedAndNonEditableRecordsNeverMutateOrEvaluateWritePermission() {
        for (String status : List.of("EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED")) {
            Map<String, Object> old = row(status, "unchanged", "2026-04-10");
            when(mapper.lock(any())).thenReturn(old);
            assertThatThrownBy(() -> service.update(88L, request("new", "2026-04-10"), faculty, "request"))
                    .isInstanceOf(ConflictException.class);
            assertThat(old.get("achievementName")).isEqualTo("unchanged");
        }
        verify(mapper, never()).update(any());
        verify(mapper, never()).history(any());
        verifyNoInteractions(permissions);
    }

    @Test
    void finalizationAndInactiveInputPeriodRejectBeforeMutation() {
        when(guard.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        assertThatThrownBy(() -> service.create(request("new", "2026-04-10"), faculty, "request"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("CONFIRMED_DATA_LOCKED");
        when(guard.countActiveInputPeriods("2026", 101L)).thenReturn(0);
        assertThatThrownBy(() -> service.create(request("new", "2026-04-10"), faculty, "request"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("PERIOD_NOT_ACTIVE");
        verify(mapper, never()).insert(any());
    }

    @Test
    void invalidItemAndUnownedAttachmentNeverWrite() {
        when(mapper.managementItemCount(any())).thenReturn(2);
        assertThatThrownBy(() -> service.create(request("new", "2026-04-10"), faculty, "request"))
                .isInstanceOf(BusinessValidationException.class);
        when(mapper.managementItemCount(any())).thenReturn(1);
        EmploymentRateAchievementRequest request = new EmploymentRateAchievementRequest("item", LocalDate.parse("2026-04-10"),
                "new", List.of("../not-a-file"));
        assertThatThrownBy(() -> service.create(request, faculty, "request")).isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insert(any());
    }

    @Test
    void multiRoleScopeAndCountUseIdenticalFiltersAndDetailScope() {
        CurrentUser multi = user("R01", "R02", "R04");
        when(mapper.list(any())).thenReturn(List.of(row("DRAFT", "listed", "2026-04-10")));
        when(mapper.count(any())).thenReturn(1L);
        Map<String, Object> result = service.list(Map.of("page", 0, "pageSize", 50, "managementItemCode", "item"), multi);
        assertThat(result.get("totalElements")).isEqualTo(1L);
        ArgumentCaptor<Map<String, Object>> list = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> count = ArgumentCaptor.forClass(Map.class);
        verify(mapper).list(list.capture());
        verify(mapper).count(count.capture());
        assertThat(count.getValue()).isEqualTo(list.getValue()).containsEntry("self", true)
                .containsEntry("department", true).containsEntry("certification", true)
                .containsEntry("managementItemCode", "item").containsEntry("pageOffset", 0L);
        when(mapper.find(any())).thenReturn(null);
        assertThatThrownBy(() -> service.detail(88L, multi)).isInstanceOf(NotFoundException.class);
        verify(mapper).find(argThat(query -> Boolean.TRUE.equals(query.get("self"))
                && Boolean.TRUE.equals(query.get("department")) && Boolean.TRUE.equals(query.get("certification"))));
    }

    @Test
    void anotherOwnerCannotBeUpdatedEvenWhenReaderHasUnionScope() {
        Map<String, Object> old = row("DRAFT", "old", "2026-04-10");
        old.put("teacherUserId", 202L);
        when(mapper.lock(any())).thenReturn(old);
        assertThatThrownBy(() -> service.update(88L, request("new", "2026-04-10"), user("R01", "R02"), "request"))
                .isInstanceOf(ForbiddenException.class);
        verify(mapper, never()).update(any());
    }

    @Test
    void policyBlockDoesNotEvenInvokeJobPersistence() {
        assertThatThrownBy(() -> service.createJob(new EmploymentRateBulkJobRequest("2026", "GENERATE", Map.of()), user("R07")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("POLICY_NOT_APPROVED");
        verifyNoInteractions(mapper);
    }

    @Test
    void realWorkbookWithOneErrorPreservesDiagnosticsAndFilesButCreatesNoBusinessRows() throws Exception {
        excelSetup();
        when(mapper.employee("invalid")).thenReturn(null);
        byte[] bytes = EmploymentRateXlsxCodec.write(List.of(columns(),
                List.of("E0101", "item", "2026-04-10", "normal", ""),
                List.of("invalid", "item", "2026-04-10", "error", "")));
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "input.xlsx",
                EmploymentRateXlsxCodec.CONTENT_TYPE, bytes), user("R07"), "excel-request"))
                .isInstanceOf(BusinessValidationException.class);
        ArgumentCaptor<Map<String, Object>> upload = ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertUpload(upload.capture());
        assertThat(upload.getValue()).containsEntry("validationStatus", "REJECTED").containsEntry("totalCount", 2)
                .containsEntry("successCount", 1).containsEntry("errorCount", 1).containsEntry("savedCount", 0);
        assertThat(storage.read(upload.getValue().get("fileToken").toString(), 101L)).isEqualTo(bytes);
        assertThat(EmploymentRateXlsxCodec.read(storage.read(upload.getValue().get("errorFileId").toString(), 101L)))
                .hasSize(2);
        verify(mapper).insertUploadHistory(any());
        verify(mapper).insertError(any());
        verify(mapper, times(2)).insertStaging(any());
        verify(mapper, never()).insert(any());
        verify(mapper, never()).update(any());
    }

    @Test
    void duplicateExistingOrWithinWorkbookIsDiagnosticNotAnUpsert() {
        excelSetup();
        when(mapper.duplicate(any())).thenReturn(1);
        byte[] bytes = EmploymentRateXlsxCodec.write(List.of(columns(), List.of("E0101", "item", "2026-04-10", "old", "")));
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "input.xlsx",
                EmploymentRateXlsxCodec.CONTENT_TYPE, bytes), user("R07"), "request"))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper).insertError(argThat(row -> row.get("errorReason").toString().contains("중복")));
        verify(mapper, never()).insert(any());
        verify(mapper, never()).update(any());
    }

    @Test
    void normalUploadStagesZeroBusinessRowsAndFinalRowCommitConflictPreventsEarlierInsert() throws Exception {
        excelSetup();
        byte[] bytes = EmploymentRateXlsxCodec.write(List.of(columns(), List.of("E0101", "item", "2026-04-10", "normal", "")));
        Map<String, Object> result = service.upload(new MockMultipartFile("file", "input.xlsx",
                EmploymentRateXlsxCodec.CONTENT_TYPE, bytes), user("R07"), "request");
        assertThat(result).containsEntry("savedCount", 0).containsEntry("errorCount", 0);
        verify(mapper, never()).insert(any());
        when(mapper.lockUpload(any())).thenReturn(Map.of("validationStatus", "VALIDATED"));
        Map<String, Object> payload = Map.of("teacherUserId", 101L, "organizationCode", "department", "evaluationYear", "2026",
                "managementItemCode", "item", "achievementDate", "2026-04-10", "achievementName", "normal", "attachmentIds", List.of());
        when(mapper.staging("selected-upload")).thenReturn(List.of(Map.of("payload", json.writeValueAsString(payload)),
                Map.of("payload", json.writeValueAsString(payload))));
        when(mapper.duplicate(any())).thenReturn(0, 1);
        assertThatThrownBy(() -> service.commit("selected-upload", user("R07"), "request"))
                .isInstanceOf(ConflictException.class);
        verify(mapper, never()).insert(any());
        verify(mapper, never()).commitUpload(any());
    }

    @Test
    void workbookRoundtripPreservesTextAndRejectsCsvOrXmlEntityPayload() {
        List<List<String>> rows = List.of(List.of("교번", "실적명"), List.of("E0101", "=SUM(1,2) & <값>"));
        assertThat(EmploymentRateXlsxCodec.read(EmploymentRateXlsxCodec.write(rows))).isEqualTo(rows);
        assertThatThrownBy(() -> EmploymentRateXlsxCodec.read("교번,실적명\nE0101,test".getBytes()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void excelSetup() {
        when(mapper.template()).thenReturn(Map.of("templateId", "template", "templateVersion", "v1"));
        when(mapper.templateColumns("template")).thenReturn(columns());
        when(mapper.employee("E0101")).thenReturn(Map.of("teacherUserId", 101L));
        when(mapper.uploadScope(any())).thenReturn(1);
    }

    private List<String> columns() {
        return List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조");
    }

    private Map<String, Object> row(String status, String name, String date) {
        Map<String, Object> row = new HashMap<>();
        row.put("achievementId", 88L);
        row.put("teacherUserId", 101L);
        row.put("organizationCode", "department");
        row.put("evaluationYear", "2026");
        row.put("managementItemCode", "item");
        row.put("achievementStatus", status);
        row.put("achievementName", name);
        row.put("achievementDate", date);
        row.put("attachmentIds", "[]");
        return row;
    }

    private EmploymentRateAchievementRequest request(String name, String date) {
        return new EmploymentRateAchievementRequest("item", LocalDate.parse(date), name, List.of());
    }

    private static CurrentUser user(String... roles) {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of(roles), List.of());
    }

    /** Same synchronization-aware DB-free manager used by foundation file tests. */
    private static class SynchronizingTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() { return new Object(); }
        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override
        protected void doCommit(DefaultTransactionStatus status) { }
        @Override
        protected void doRollback(DefaultTransactionStatus status) { }
    }
}
