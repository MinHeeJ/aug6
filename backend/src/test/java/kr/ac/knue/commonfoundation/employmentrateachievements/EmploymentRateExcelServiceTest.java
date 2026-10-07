package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class EmploymentRateExcelServiceTest {
    private final EmploymentRateExcelRepository repository = mock(EmploymentRateExcelRepository.class);
    private final EducationAchievementGuardMapper guards = mock(EducationAchievementGuardMapper.class);
    private final FileStoragePort storage = mock(FileStoragePort.class);
    private final EmploymentRateAchievementService achievements = mock(EmploymentRateAchievementService.class);
    private final EmploymentRateWorkbookCodec codec = new EmploymentRateWorkbookCodec();
    private final CurrentUser user = new CurrentUser(107L, "operator", "E107", "담당자", List.of("R07"), List.of());
    private final EmploymentRateExcelRepository.Template template = new EmploymentRateExcelRepository.Template(
            "EMPLOYMENT-RATE-ACHIEVEMENT-v1", "v1.0", EmploymentRateExcelService.COLUMNS);
    private EmploymentRateExcelService service;
    private RecordingTransactionManager transactions;

    @BeforeEach
    void setup() {
        transactions = new RecordingTransactionManager();
        service = new EmploymentRateExcelService(repository, guards, achievements, new ObjectMapper(), storage,
                transactions);
        when(repository.currentTemplate()).thenReturn(template);
        when(repository.findTarget("0001", 107L)).thenReturn(
                new EmploymentRateExcelRepository.Target(101L, "ORG"));
        when(repository.countItem("FR-032", "2026", LocalDate.parse("2026-03-01"))).thenReturn(1);
        when(guards.countActiveInputPeriods("2026", 101L)).thenReturn(1);
        when(guards.countEvaluationDatePeriods("2026", 101L, LocalDate.parse("2026-03-01"))).thenReturn(1);
        when(storage.save(anyLong(), any(), any())).thenReturn("original", "errors");
    }

    @Test
    void validationStagesNormalRowsWithoutMaterializingAnyAchievement() {
        var result = service.validateExcelUpload(file(List.of("0001", "FR-032", "2026-03-01", "실적", "")), user);
        assertThat(result.savedCount()).isZero();
        assertThat(result.successCount()).isEqualTo(1);
        verifyNoInteractions(achievements);
        verify(repository).insertHistory(result.uploadId(), 1, 1, 0, 0, 107L);
    }

    @Test
    void errorsInLaterRowsBlockWholeFileAndRetainIndependentColumnDiagnostics() {
        var result = service.validateExcelUpload(file(
                List.of("0001", "FR-032", "2026-03-01", "실적", ""),
                List.of("0001", "FR-032", "bad-date", "", "")), user);
        assertThat(result.savedCount()).isZero();
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.errors()).extracting(EmploymentRateExcelService.Error::columnName)
                .contains("업적발생일", "실적명");
        verifyNoInteractions(achievements);
        verify(repository).insertErrorDownload(eq(result.uploadId()), eq("errors"), eq(107L));
    }

    @Test
    void duplicateIdentityIgnoresNameAndNeverUpserts() {
        var result = service.validateExcelUpload(file(
                List.of("0001", "FR-032", "2026-03-01", "실적1", ""),
                List.of("0001", "FR-032", "2026-03-01", "실적2", "")), user);
        assertThat(result.errors()).extracting(EmploymentRateExcelService.Error::errorCode).contains("DUPLICATE");
        verifyNoInteractions(achievements);
    }

    @Test
    void operatorRoleAndScopeAreRequired() {
        CurrentUser admin = new CurrentUser(1L, "admin", "E1", "관리자", List.of("R09"), List.of());
        assertThatThrownBy(() -> service.validateExcelUpload(file(), admin)).isInstanceOf(ForbiddenException.class);
        var result = service.validateExcelUpload(file(List.of("OTHER", "FR-032", "2026-03-01", "실적", "")), user);
        assertThat(result.errors()).extracting(EmploymentRateExcelService.Error::errorCode).contains("TARGET_OUT_OF_SCOPE");
    }

    @Test
    void explicitConfirmationAndOwnerBoundValidatedStateAreRequired() {
        assertThatThrownBy(() -> service.commitExcelUpload("up", user, false, "req"))
                .isInstanceOf(ConflictException.class);
        when(repository.findUpload("up", 107L, true)).thenReturn(
                new EmploymentRateExcelRepository.Upload("up", "original", template.templateId(), "COMMITTED", 1, 1, 0, 1));
        assertThatThrownBy(() -> service.commitExcelUpload("up", user, true, "req"))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(achievements);
    }

    @Test
    void revalidationOfFinalizationPreventsFirstDomainWrite() throws Exception {
        when(repository.findUpload("up", 107L, true)).thenReturn(
                new EmploymentRateExcelRepository.Upload("up", "original", template.templateId(), "VALIDATED", 1, 1, 0, 0));
        String payload = new ObjectMapper().writeValueAsString(Map.of(
                "cells", List.of("0001", "FR-032", "2026-03-01", "실적", ""),
                "templateVersion", "v1.0", "templateId", template.templateId(),
                "teacherUserId", 101L, "organizationCode", "ORG", "evaluationYear", "2026"));
        when(repository.staging("up")).thenReturn(List.of(new EmploymentRateExcelRepository.Staged(3, payload, "NORMAL")));
        when(guards.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        assertThatThrownBy(() -> service.commitExcelUpload("up", user, true, "req"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("CONFIRMED_DATA_LOCKED");
        verifyNoInteractions(achievements);
        verify(repository, never()).finish(any(), anyLong(), anyInt());
    }

    @Test
    void successfulCommitMaterializesOnlyAfterAllRowsAreRevalidated() throws Exception {
        when(repository.findUpload("up", 107L, true)).thenReturn(
                new EmploymentRateExcelRepository.Upload("up", "original", template.templateId(), "VALIDATED", 1, 1, 0, 0));
        String payload = new ObjectMapper().writeValueAsString(Map.of(
                "cells", List.of("0001", "FR-032", "2026-03-01", "실적", ""),
                "templateVersion", "v1.0", "templateId", template.templateId(),
                "teacherUserId", 101L, "organizationCode", "ORG", "evaluationYear", "2026"));
        when(repository.staging("up")).thenReturn(List.of(new EmploymentRateExcelRepository.Staged(3, payload, "NORMAL")));
        assertThat(service.commitExcelUpload("up", user, true, "req").savedCount()).isEqualTo(1);
        verify(achievements).createImported(eq(user), any(EmploymentRateAchievementRequest.class), eq(101L), eq("2026"), eq("req"));
        verify(repository).finish("up", 107L, 1);
    }

    @Test
    void oneErroneousRowRetainsOriginalReportErrorsAndHistoryAndCannotCommit() throws Exception {
        MockMultipartFile original = file(
                List.of("0001", "FR-032", "2026-03-01", "정상", ""),
                List.of("0001", "FR-032", "bad-date", "", ""));
        var result = service.validateExcelUpload(original, user, "stage-req");
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errorCount()).isEqualTo(1); // two column errors, one invalid row
        assertThat(result.errors()).hasSize(2);
        verify(repository).insertUpload(result.uploadId(), template.templateId(), "original", "employment.xlsx", "REJECTED", 107L);
        verify(repository).insertStaging(eq(result.uploadId()), eq(3), anyString(), eq("NORMAL"));
        verify(repository).insertStaging(eq(result.uploadId()), eq(4), anyString(), eq("ERROR"));
        result.errors().forEach(error -> verify(repository).insertError(result.uploadId(), error));
        verify(repository).insertHistory(result.uploadId(), 2, 1, 1, 0, 107L);
        var retained = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(storage).save(eq(107L), eq("original"), retained.capture());
        assertThat(retained.getValue()).isEqualTo(original.getBytes());
        var report = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(storage).save(eq(107L), eq("errors"), report.capture());
        var reportRows = codec.read(report.getValue());
        assertThat(reportRows).hasSize(3);
        assertThat(reportRows.get(1)).contains("4", "INVALID_NAME");
        assertThat(reportRows.get(2)).contains("4", "INVALID_DATE", "bad-date");
        verify(repository).insertErrorDownload(result.uploadId(), "errors", 107L);
        assertThat(transactions.commits).isEqualTo(1);
        assertThat(transactions.propagations).containsOnly(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verify(storage, never()).delete(anyLong(), anyString());

        var rejected = new EmploymentRateExcelRepository.Upload(result.uploadId(), "original", template.templateId(), "REJECTED", 2, 1, 1, 0);
        when(repository.findUpload(result.uploadId(), 107L, false)).thenReturn(rejected);
        when(repository.findUpload(result.uploadId(), 107L, true)).thenReturn(rejected);
        when(repository.errors(result.uploadId())).thenReturn(result.errors());
        when(repository.errorFileToken(result.uploadId(), 107L)).thenReturn("errors");
        when(storage.read(107L, "errors")).thenReturn(report.getValue());
        var histories = List.<Map<String, Object>>of(Map.of("uploadId", result.uploadId(), "savedCount", 0, "errorCount", 1));
        when(repository.histories(107L)).thenReturn(histories);
        assertThat(service.listExcelErrors(result.uploadId(), user)).isEqualTo(result.errors());
        assertThat(service.listExcelHistories(user)).isEqualTo(histories);
        assertThat(service.downloadExcelErrors(result.uploadId(), user).content()).isEqualTo(report.getValue());
        assertThatThrownBy(() -> service.commitExcelUpload(result.uploadId(), user, true, "commit-req"))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(achievements);
        verify(repository, never()).finish(anyString(), anyLong(), anyInt());
        verify(storage, never()).delete(anyLong(), anyString());
    }

    @Test
    void normalStageToCommitUsesActualDomainConstructorAndCreatesEveryRowWithAuditHistory() {
        var mapper = mock(EmploymentRateAchievementMapper.class);
        var domain = new EmploymentRateAchievementService(mapper,
                mock(kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService.class),
                guards, repository, storage, new ObjectMapper());
        service = new EmploymentRateExcelService(repository, guards, domain, new ObjectMapper(), storage, transactions);
        when(repository.findTarget("0002", 107L)).thenReturn(new EmploymentRateExcelRepository.Target(102L, "ORG"));
        when(guards.countActiveInputPeriods("2026", 102L)).thenReturn(1);
        when(guards.countCertificationScope(107L, 101L)).thenReturn(1);
        when(guards.countCertificationScope(107L, 102L)).thenReturn(1);
        when(repository.countItem("FR-032", "2026", LocalDate.parse("2026-06-30"))).thenReturn(1);
        when(storage.exists(102L, "attachment-102")).thenReturn(true);
        when(mapper.organization(anyLong())).thenReturn("ORG");
        var inserted = new java.util.ArrayList<Map<String, Object>>();
        doAnswer(call -> {
            Map<String, Object> row = call.getArgument(0);
            row.put("achievementId", 201L + inserted.size());
            var saved = new java.util.LinkedHashMap<>(row);
            // Mapper read results expose strings, not Java time objects, to the plain ObjectMapper.
            saved.put("achievementDate", row.get("achievementDate").toString());
            inserted.add(saved);
            return null;
        }).when(mapper).insert(any());
        when(mapper.find(anyLong(), eq(false))).thenAnswer(call -> inserted.stream()
                .filter(row -> row.get("achievementId").equals(call.getArgument(0))).findFirst().orElse(null));

        var result = service.validateExcelUpload(file(
                List.of("0001", "FR-032", "2026-03-01", "첫 실적", ""),
                List.of("0002", "FR-032", "2026-03-01", "둘째 실적", "attachment-102")), user, "stage-req");
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.savedCount()).isZero();
        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).extracting(EmploymentRateExcelService.Error::errorCode).containsExactly("OUTSIDE_EVALUATION_PERIOD");
        verifyNoInteractions(mapper);
        var rows = org.mockito.ArgumentCaptor.forClass(String.class);
        var numbers = org.mockito.ArgumentCaptor.forClass(Integer.class);
        verify(repository, times(2)).insertStaging(eq(result.uploadId()), numbers.capture(), rows.capture(), eq("NORMAL"));
        var staged = new java.util.ArrayList<EmploymentRateExcelRepository.Staged>();
        for (int i = 0; i < rows.getAllValues().size(); i++) {
            staged.add(new EmploymentRateExcelRepository.Staged(numbers.getAllValues().get(i), rows.getAllValues().get(i), "NORMAL"));
        }
        when(repository.staging(result.uploadId())).thenReturn(staged);
        when(repository.findUpload(result.uploadId(), 107L, true)).thenReturn(
                new EmploymentRateExcelRepository.Upload(result.uploadId(), "original", template.templateId(), "VALIDATED", 2, 2, 0, 0));
        clearInvocations(repository, guards);
        var committed = service.commitExcelUpload(result.uploadId(), user, true, "commit-req");
        assertThat(committed.savedCount()).isEqualTo(2);
        assertThat(committed.warnings()).isEqualTo(result.warnings());
        assertThat(inserted).extracting(row -> row.get("teacherUserId")).containsExactly(101L, 102L);
        assertThat(inserted).extracting(row -> row.get("achievementName")).containsExactly("첫 실적", "둘째 실적");
        assertThat(inserted.get(0).get("attachmentRef")).isNull();
        assertThat(inserted.get(1).get("attachmentRef")).isEqualTo("attachment-102");
        assertThat(inserted).allSatisfy(row -> {
            assertThat(row.get("actorId")).isEqualTo(107L);
            assertThat(row.get("evaluationYear")).isEqualTo("2026");
            assertThat(row.get("requestId")).isEqualTo("commit-req");
        });
        var order = inOrder(repository, guards, mapper);
        order.verify(repository).lockGuards();
        order.verify(repository).findUpload(result.uploadId(), 107L, true);
        order.verify(repository).staging(result.uploadId());
        order.verify(guards).countActiveInputPeriods("2026", 102L); // final row checked before first domain write
        order.verify(mapper).insert(any());
        order.verify(mapper).statusHistory(201L, 107L, "commit-req");
        order.verify(mapper).history(eq(201L), isNull(), anyString(), eq(107L), eq("commit-req"), eq("CREATE"));
        order.verify(mapper).insert(any());
        order.verify(mapper).statusHistory(202L, 107L, "commit-req");
        order.verify(mapper).history(eq(202L), isNull(), anyString(), eq(107L), eq("commit-req"), eq("CREATE"));
        order.verify(repository).finish(result.uploadId(), 107L, 2);
        verify(mapper, never()).update(any());
        assertThat(transactions.commits).isEqualTo(2);
        assertThat(transactions.rollbacks).isZero();
    }

    @Test
    void lateRowDatabaseDuplicateBlocksEveryWriteAndLeavesStagingAndDiagnosticsIntact() throws Exception {
        prepareTwoRows();
        when(repository.countDuplicate(102L, "2026", "FR-032", LocalDate.parse("2026-03-01"))).thenReturn(1);
        assertCommitBlocked("DUPLICATE");
    }

    @Test
    void duplicateRowsWithinStagingCannotCausePartialCommitOrUpsert() throws Exception {
        prepareTwoRows();
        when(repository.staging("up")).thenReturn(List.of(staged(3, "0001", 101L), staged(4, "0001", 101L)));
        assertCommitBlocked("DUPLICATE");
    }

    @Test
    void changedTargetSnapshotBlocksWholeCommitBeforeDomainCalls() throws Exception {
        prepareTwoRows();
        when(repository.findTarget("0002", 107L)).thenReturn(new EmploymentRateExcelRepository.Target(102L, "NEW-ORG"));
        assertCommitBlocked("TARGET_CHANGED");
    }

    @Test
    void closedPeriodOnLastRowPreventsAnyEarlierRowBeingSaved() throws Exception {
        prepareTwoRows();
        when(guards.countActiveInputPeriods("2026", 102L)).thenReturn(0);
        assertCommitBlocked("PERIOD_NOT_ACTIVE");
    }

    @Test
    void incompleteOrMalformedStagingCannotCommit() throws Exception {
        prepareTwoRows();
        when(repository.staging("up")).thenReturn(List.of(staged(3, "0001", 101L)));
        assertCommitBlocked("INCOMPLETE_STAGING");
        when(repository.staging("up")).thenReturn(List.of(staged(3, "0001", 101L),
                new EmploymentRateExcelRepository.Staged(4, "not-json", "NORMAL")));
        assertCommitBlocked("INVALID_STAGING");
    }

    @Test
    void stageRollbackCompensatesOnlyNewRetainedFilesUsingRealSynchronizations() {
        doThrow(new IllegalStateException("history write failed")).when(repository)
                .insertErrorDownload(anyString(), eq("errors"), eq(107L));
        assertThatThrownBy(() -> service.validateExcelUpload(file(List.of("0001", "FR-032", "bad-date", "", "")), user))
                .isInstanceOf(IllegalStateException.class).hasMessage("history write failed");
        verify(storage).delete(107L, "original");
        verify(storage).delete(107L, "errors");
        assertThat(transactions.commits).isZero();
        assertThat(transactions.rollbacks).isEqualTo(1);
        assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
        verifyNoInteractions(achievements);
    }

    @Test
    void unownedErrorsAndDownloadsCannotReadRetainedFiles() {
        assertThatThrownBy(() -> service.listExcelErrors("other", user))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.NotFoundException.class);
        assertThatThrownBy(() -> service.downloadExcelErrors("other", user))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.NotFoundException.class);
        verify(repository, never()).errors("other");
        verifyNoInteractions(storage, achievements);
    }

    @Test
    void changedCurrentTemplateBlocksCommitBeforeAnyDomainWrite() throws Exception {
        prepareTwoRows();
        when(repository.currentTemplate()).thenReturn(
                new EmploymentRateExcelRepository.Template("new-template", "v2", EmploymentRateExcelService.COLUMNS));
        assertCommitBlocked("INVALID_TEMPLATE");
    }

    @Test
    void missingTargetAndAttachmentOwnershipAreRetainedAsIndependentStageDiagnostics() {
        var result = service.validateExcelUpload(file(
                List.of("0001", "FR-032", "2026-03-01", "실적", "foreign-ref"),
                List.of("OTHER", "FR-032", "2026-03-01", "실적", "")), user);
        assertThat(result.errorCount()).isEqualTo(2);
        assertThat(result.successCount()).isZero();
        assertThat(result.savedCount()).isZero();
        assertThat(result.errors()).extracting(EmploymentRateExcelService.Error::errorCode)
                .containsExactly("INVALID_ATTACHMENT", "TARGET_OUT_OF_SCOPE");
        verify(storage).exists(101L, "foreign-ref");
        verify(repository).insertHistory(result.uploadId(), 2, 0, 2, 0, 107L);
        verifyNoInteractions(achievements);
    }

    @Test
    void disguisedCsvIsRetainedAsFileDiagnosticAndNumericDatesAreNotAcceptedAsIsoDates() {
        var result = service.validateExcelUpload(new MockMultipartFile("file", "disguised.xlsx",
                EmploymentRateWorkbookCodec.MIME, "교번,실적명".getBytes(java.nio.charset.StandardCharsets.UTF_8)), user);
        assertThat(result.errors()).extracting(EmploymentRateExcelService.Error::errorCode).containsExactly("INVALID_XLSX");
        assertThat(result.savedCount()).isZero();
        assertThat(result.errorDownloadUrl()).endsWith("/errors/download");
        verify(repository).insertUpload(eq(result.uploadId()), eq(template.templateId()), eq("original"),
                eq("disguised.xlsx"), eq("REJECTED"), eq(107L));
        var serial = service.validateExcelUpload(file(List.of("0001", "FR-032", "46082", "실적", "")), user);
        assertThat(serial.errors()).extracting(EmploymentRateExcelService.Error::errorCode).containsExactly("INVALID_DATE");
        verifyNoInteractions(achievements);
    }

    @Test
    void invalidFilenameAndOversizedMultipartRejectBeforeReadingOrOpeningTransaction() throws Exception {
        var invalid = new MockMultipartFile("file", "rates.csv", "text/csv", new byte[] {1});
        assertThatThrownBy(() -> service.validateExcelUpload(invalid, user))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.BusinessValidationException.class);
        var oversized = mock(org.springframework.web.multipart.MultipartFile.class);
        when(oversized.getSize()).thenReturn((long) EmploymentRateWorkbookCodec.MAX_INPUT_BYTES + 1);
        assertThatThrownBy(() -> service.validateExcelUpload(oversized, user))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.BusinessValidationException.class);
        verify(oversized, never()).getInputStream();
        assertThat(transactions.propagations).isEmpty();
        verifyNoInteractions(repository, guards, achievements, storage);
    }

    @Test
    void downloadedTemplateIsRealCurrentWorkbookAndNoDomainWrites() {
        var download = service.downloadExcelTemplate(user);
        assertThat(download.contentType()).isEqualTo(EmploymentRateWorkbookCodec.MIME);
        assertThat(codec.read(download.content())).containsExactly(
                List.of("templateId", template.templateId(), "templateVersion", template.templateVersion()), template.columns());
        verifyNoInteractions(storage, achievements);
    }

    private void prepareTwoRows() throws Exception {
        when(repository.findUpload("up", 107L, true)).thenReturn(
                new EmploymentRateExcelRepository.Upload("up", "original", template.templateId(), "VALIDATED", 2, 2, 0, 0));
        when(repository.findTarget("0002", 107L)).thenReturn(new EmploymentRateExcelRepository.Target(102L, "ORG"));
        when(guards.countActiveInputPeriods("2026", 102L)).thenReturn(1);
        when(repository.staging("up")).thenReturn(List.of(staged(3, "0001", 101L), staged(4, "0002", 102L)));
    }

    private EmploymentRateExcelRepository.Staged staged(int number, String employee, long teacher) throws Exception {
        return new EmploymentRateExcelRepository.Staged(number, new ObjectMapper().writeValueAsString(Map.of(
                "cells", List.of(employee, "FR-032", "2026-03-01", "실적", ""),
                "templateVersion", template.templateVersion(), "templateId", template.templateId(),
                "teacherUserId", teacher, "organizationCode", "ORG", "evaluationYear", "2026")), "NORMAL");
    }

    private void assertCommitBlocked(String code) {
        assertThatThrownBy(() -> service.commitExcelUpload("up", user, true, "req"))
                .isInstanceOf(ConflictException.class).hasMessageContaining(code);
        verifyNoInteractions(achievements);
        verify(repository, never()).finish(anyString(), anyLong(), anyInt());
        verify(repository, never()).insertStaging(anyString(), anyInt(), anyString(), anyString());
        verify(repository, never()).insertError(anyString(), any());
        verifyNoInteractions(storage);
        assertThat(transactions.commits).isZero();
    }

    @SafeVarargs
    private final MockMultipartFile file(List<String>... rows) {
        var grid = new java.util.ArrayList<List<String>>();
        grid.add(List.of("templateId", template.templateId(), "templateVersion", template.templateVersion()));
        grid.add(EmploymentRateExcelService.COLUMNS);
        grid.addAll(List.of(rows));
        return new MockMultipartFile("file", "employment.xlsx", EmploymentRateWorkbookCodec.MIME, codec.write(grid));
    }

    /** Uses Spring's synchronization lifecycle without any database or transaction resources. */
    private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        int commits;
        int rollbacks;
        final List<Integer> propagations = new java.util.ArrayList<>();
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            propagations.add(definition.getPropagationBehavior());
        }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
