package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.storage.FileStoragePort;
import kr.ac.knue.commonfoundation.storage.LocalFileStorageAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/** Real codec and real validation/materialization orchestration with a synchronization-aware DB-free transaction manager. */
class EmploymentRateExcelServiceTest {
    private EmploymentRateAchievementMapper mapper;
    private EducationAchievementGuardMapper guards;
    private EmploymentRateExcelService excel;
    private FileStoragePort files;
    private MemoryTransactions transactions;
    private final List<Map<String, Object>> persisted = new ArrayList<>();
    private final List<byte[]> savedBytes = new ArrayList<>();
    private static final List<String> HEADER = List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조");
    private final CurrentUser actor = new CurrentUser(107L, "operator", "E107", "담당자", List.of("R07"), List.of());

    @BeforeEach
    void setup() {
        mapper = mock(EmploymentRateAchievementMapper.class);
        guards = mock(EducationAchievementGuardMapper.class);
        files = mock(FileStoragePort.class);
        transactions = new MemoryTransactions(persisted);
        EmploymentRateAchievementService service = new EmploymentRateAchievementService(
                mapper, guards, new ObjectMapper().findAndRegisterModules(), files);
        excel = new EmploymentRateExcelService(mapper, service, files, transactions);
        when(mapper.templateColumns()).thenReturn(HEADER);
        when(mapper.templateId()).thenReturn("EMPLOYMENT-RATE-TEMPLATE-V1");
        when(mapper.teacher("E101")).thenReturn(101L);
        when(mapper.organizations(101L)).thenReturn(List.of("DEPT"));
        when(mapper.years("DEPT")).thenReturn(List.of("2026"));
        when(mapper.departmentScope(107L, "DEPT")).thenReturn(1);
        when(mapper.managementItems("2026")).thenReturn(List.of(Map.of(
                "managementItemCode", "ITEM", "teacherEditableYn", "Y")));
        when(guards.countActiveInputPeriods("2026", 101L)).thenReturn(1);
        when(files.save(any())).thenAnswer(call -> {
            savedBytes.add(call.getArgument(0));
            return java.util.UUID.randomUUID().toString();
        });
        doAnswer(call -> {
            Map<String, Object> row = call.getArgument(0);
            row.put("achievementId", (long) persisted.size() + 1);
            persisted.add(new LinkedHashMap<>(row));
            return 1;
        }).when(mapper).insert(any());
    }

    @Test
    void realXlsxRoundTripCommitsEveryRowAndRetainsUploadHistory() {
        var result = excel.upload(file(validRows()), actor, "req-excel");
        assertThat(result.savedCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(persisted).hasSize(2);
        verify(mapper, times(2)).initialStatus(any());
        verify(mapper, times(8)).audit(any());
        verify(mapper).history(argThat(entry -> entry.get("saved").equals(2)));
        assertThat(transactions.commits).isEqualTo(2);
        assertThat(savedBytes).hasSize(1);
        assertThat(EmploymentRateXlsxCodec.read(savedBytes.get(0))).isEqualTo(validRows());
    }

    @Test
    void oneInvalidLastRowLeavesZeroWritesAndRealErrorFileAndHistory() {
        List<List<String>> rows = new ArrayList<>(validRows());
        rows.set(2, List.of("E101", "ITEM", "bad-date", "불량 행", ""));
        assertThatThrownBy(() -> excel.upload(file(rows), actor, "req-error"))
                .isInstanceOf(BusinessValidationException.class).satisfies(exception -> {
                    var fields = ((BusinessValidationException) exception).fields();
                    assertThat(fields).anySatisfy(field -> assertThat(field.field()).isEqualTo("errorFileToken"));
                    assertThat(fields).anySatisfy(field -> assertThat(field.field()).isEqualTo("row.3"));
                });
        assertThat(persisted).isEmpty();
        verify(mapper, never()).insert(any());
        verify(mapper).history(argThat(entry -> entry.get("saved").equals(0)));
        verify(mapper).errorFile(any());
        assertThat(savedBytes).hasSize(2);
        assertThat(EmploymentRateXlsxCodec.read(savedBytes.get(1)).get(1).get(0)).isEqualTo("row.3");
    }

    @Test
    void fileInternalDuplicateIsNotAnUpsert() {
        var repeated = List.of(HEADER, validRows().get(1), validRows().get(1));
        assertThatThrownBy(() -> excel.upload(file(repeated), actor, "duplicate"))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insert(any());
        verify(mapper, never()).update(any());
        verify(mapper).history(any());
    }

    @Test
    void databaseDuplicateKeepsOriginalValues() {
        when(mapper.duplicate(any())).thenReturn(1);
        assertThatThrownBy(() -> excel.upload(file(validRows()), actor, "db-duplicate"))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insert(any());
        verify(mapper, never()).update(any());
    }

    @Test
    void conflictOnLastRevalidationWritesNothing() {
        when(mapper.duplicate(any())).thenReturn(0, 0, 0, 1);
        assertThatThrownBy(() -> excel.upload(file(validRows()), actor, "revalidation"))
                .isInstanceOf(BusinessValidationException.class);
        assertThat(persisted).isEmpty();
        assertThat(transactions.rollbacks).isEqualTo(1);
        verify(mapper, never()).insert(any());
        verify(mapper, times(2)).history(argThat(entry -> entry.get("saved").equals(0)));
        verify(mapper).uploadStatus(argThat(entry -> entry.get("status").equals("REJECTED")));
    }

    @Test
    void concurrentUniqueConflictDuringSecondInsertRollsBackEarlierRow() {
        doAnswer(call -> {
            Map<String, Object> row = call.getArgument(0);
            if (persisted.size() == 1) throw new DuplicateKeyException("unique conflict");
            row.put("achievementId", 1L);
            persisted.add(new LinkedHashMap<>(row));
            return 1;
        }).when(mapper).insert(any());
        assertThatThrownBy(() -> excel.upload(file(validRows()), actor, "race"))
                .isInstanceOf(BusinessValidationException.class);
        assertThat(persisted).isEmpty();
        assertThat(transactions.rollbacks).isEqualTo(1);
        verify(mapper, times(2)).history(argThat(entry -> entry.get("saved").equals(0)));
        verify(mapper).uploadStatus(argThat(entry -> entry.get("status").equals("REJECTED")));
    }

    @Test
    void outOfDepartmentAndFinalizedAndInactiveTargetsNeverMaterialize() {
        when(mapper.departmentScope(107L, "DEPT")).thenReturn(0);
        assertThatThrownBy(() -> excel.upload(file(validRows()), actor, "scope"))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insert(any());
    }

    @Test
    void finalizedTargetIsRejected() {
        when(guards.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        assertThatThrownBy(() -> excel.upload(file(validRows()), actor, "confirmed"))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insert(any());
    }

    @Test
    void inactivePeriodIsRejected() {
        when(guards.countActiveInputPeriods("2026", 101L)).thenReturn(0);
        assertThatThrownBy(() -> excel.upload(file(validRows()), actor, "period"))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insert(any());
    }

    @Test
    void renamedCsvAndMalformedZipHaveRetainedDiagnostics() {
        var file = new MockMultipartFile("file", "renamed.xlsx", EmploymentRateXlsxCodec.MIME,
                "교번,관리항목코드\nE101,ITEM".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThatThrownBy(() -> excel.upload(file, actor, "renamed"))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insert(any());
        verify(mapper).history(any());
        verify(mapper).errorFile(any());
    }

    @Test
    void wrongFileExtensionIsRejectedRatherThanPretendingCsvIsXlsx() {
        var file = new MockMultipartFile("file", "upload.csv", "text/csv", new byte[]{1});
        assertThatThrownBy(() -> excel.upload(file, actor, "extension"))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insert(any());
    }

    @Test
    void diagnosticRollbackCompensatesNewFiles() {
        doThrow(new IllegalStateException("DB failure")).when(mapper).history(any());
        assertThatThrownBy(() -> excel.upload(new MockMultipartFile("file", "bad.xlsx", "application/octet-stream",
                        new byte[]{1, 2}), actor, "diag-failure"))
                .isInstanceOf(IllegalStateException.class);
        verify(files, times(2)).delete(any());
        assertThat(persisted).isEmpty();
    }

    @Test
    void localStorageUsesOpaqueTokensAndRejectsTraversal() {
        LocalFileStorageAdapter local = new LocalFileStorageAdapter();
        String token = local.save(new byte[]{7, 8});
        try {
            assertThat(local.read(token)).containsExactly((byte) 7, (byte) 8);
            assertThat(local.size(token)).isEqualTo(2);
            assertThat(local.exists("../../etc/passwd")).isFalse();
            assertThatThrownBy(() -> local.read("../../etc/passwd")).isInstanceOf(RuntimeException.class);
        } finally {
            local.delete(token);
        }
        assertThat(local.exists(token)).isFalse();
    }

    private static MockMultipartFile file(List<List<String>> rows) {
        return new MockMultipartFile("file", "employment.xlsx", EmploymentRateXlsxCodec.MIME,
                EmploymentRateXlsxCodec.write(rows));
    }

    private static List<List<String>> validRows() {
        return List.of(HEADER, List.of("E101", "ITEM", "2026-04-11", "실적 1", ""),
                List.of("E101", "ITEM", "2026-04-12", "실적 2", ""));
    }

    /** Spring drives synchronization lifecycle; snapshots simulate rollback only at the mocked persistence boundary. */
    private static final class MemoryTransactions extends AbstractPlatformTransactionManager {
        private final List<Map<String, Object>> rows;
        int commits;
        int rollbacks;

        MemoryTransactions(List<Map<String, Object>> rows) {
            this.rows = rows;
        }

        @Override protected Object doGetTransaction() {
            return new ArrayList<>(rows);
        }

        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override protected void doRollback(DefaultTransactionStatus status) {
            rows.clear();
            rows.addAll((List<Map<String, Object>>) status.getTransaction());
            rollbacks++;
        }
    }
}
