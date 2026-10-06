package kr.ac.knue.commonfoundation.employmentrateachievements;

import static kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateExcelModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.excel.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class EmploymentRateExcelServiceTest {
    private final EmploymentRateExcelRepository repository = mock(EmploymentRateExcelRepository.class);
    private final EducationAchievementGuardMapper guards = mock(EducationAchievementGuardMapper.class);
    private final FileStoragePort storage = mock(FileStoragePort.class);
    private final FunctionPermissionService permissions = mock(FunctionPermissionService.class);
    private final ObjectMapper json = new ObjectMapper();
    private final EmploymentRateWorkbook workbook = new EmploymentRateWorkbook();
    private final EmploymentRateExcelService service = new EmploymentRateExcelService(
            repository, workbook, guards, storage, json, permissions);
    private final CurrentUser operator = user("R07");
    private final Target target = new Target(22L, "KNUE-DEPT-COMP");
    private final Template template = new Template("EMPLOYMENT-RATE-V1", "1",
            List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조"));
    private final InputRow row = new InputRow(2, "00123", "FR-032", "2025-04-10", "Employment", "");

    @BeforeEach
    void setup() {
        when(repository.currentTemplate()).thenReturn(template);
        when(repository.targets("00123")).thenReturn(List.of(target));
        when(repository.inScope(7L, target)).thenReturn(true);
        when(repository.itemRules("FR-032", "2025")).thenReturn(List.of(new ItemRule("Y", "TEXT", "Y")));
        when(guards.countActiveInputPeriods("2025", 22L)).thenReturn(1);
        when(guards.countEvaluationDatePeriods(eq("2025"), eq(22L), any())).thenReturn(1);
        when(storage.save(any(), eq(7L))).thenReturn("opaque-storage-reference");
        when(repository.ownedUpload("UPLOAD", 7L, true))
                .thenReturn(new Upload("UPLOAD", template.templateId(), "VALIDATED", "test.xlsx"));
        when(repository.stagingComplete("UPLOAD")).thenReturn(true);
    }

    @Test
    void allOperationsRejectNonR07IncludingAdministrator() {
        for (String role : List.of("R01", "R02", "R04", "R09")) {
            CurrentUser user = user(role);
            assertThatThrownBy(() -> service.downloadExcelTemplate(user)).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> service.validateExcelUpload(null, user)).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() ->
                    service.commitExcelUpload("UPLOAD", user, "req")).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> service.listExcelHistories(user)).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> service.listExcelErrors("UPLOAD", user)).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() ->
                    service.downloadExcelErrors("UPLOAD", user)).isInstanceOf(ForbiddenException.class);
        }
        assertThatThrownBy(() -> service.listExcelHistories(null)).isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void uploadValidatesWithoutSavingAndCommitReportsSavedRows() throws Exception {
        UploadResult result = service.validateExcelUpload(file(1, ""), operator);
        assertThat(result.validationStatus()).isEqualTo("VALIDATED");
        assertThat(result.savedCount()).isZero();
        assertThat(result.totalCount()).isEqualTo(1);
        verify(repository, never()).insertAchievement(any(), any(), any(), any(), anyLong(), any());
        stage(row);
        CommitResult committed = service.commitExcelUpload("UPLOAD", operator, "request");
        assertThat(committed.savedCount()).isEqualTo(1);
        verify(repository).insertAchievement(target, row, LocalDate.parse("2025-04-10"), "[]", 7L, "request");
    }

    @Test
    void duplicateFileRowsReturnPersistableErrorResultAndZeroSaved() throws Exception {
        UploadResult result = service.validateExcelUpload(file(2, ""), operator);
        assertThat(result.validationStatus()).isEqualTo("REJECTED");
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.savedCount()).isZero();
        assertThat(result.errors()).singleElement().extracting(ErrorRow::errorCode).isEqualTo("DUPLICATE");
        assertThat(result.errorDownloadUrl()).endsWith("/errors/download");
        verify(repository).persistErrorFile(eq(result.uploadId()), anyString(), eq(7L));
    }

    @Test
    void missingDataScopeCannotBeBypassedByR07() throws Exception {
        when(repository.inScope(7L, target)).thenReturn(false);
        assertThatThrownBy(() -> service.validateExcelUpload(file(1, ""), operator))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void periodAndFinalizationAreRecheckedForActualTeacherNotOperator() throws Exception {
        stage(row);
        when(guards.countActiveInputPeriods("2025", 22L)).thenReturn(0);
        assertThatThrownBy(() -> service.commitExcelUpload("UPLOAD", operator, "req"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("PERIOD_NOT_ACTIVE");
        when(guards.countActiveInputPeriods("2025", 22L)).thenReturn(1);
        when(guards.countEvaluationConfirmations(22L, "2025")).thenReturn(1);
        assertThatThrownBy(() -> service.commitExcelUpload("UPLOAD", operator, "req"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("CONFIRMED_DATA_LOCKED");
    }

    @Test
    void commitRechecksDbDuplicatesAndDoesNotAutoUpdate() throws Exception {
        stage(row);
        when(repository.duplicate(target, row, LocalDate.parse("2025-04-10"))).thenReturn(true);
        assertThatThrownBy(() -> service.commitExcelUpload("UPLOAD", operator, "req"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("DUPLICATE");
        verify(repository, never()).insertAchievement(any(), any(), any(), any(), anyLong(), any());
    }

    @Test
    void confirmedOrAlreadyCommittedUploadCannotBeReplayed() {
        when(repository.ownedUpload("UPLOAD", 7L, true))
                .thenReturn(new Upload("UPLOAD", template.templateId(), "COMMITTED", "test.xlsx"));
        assertThatThrownBy(() -> service.commitExcelUpload("UPLOAD", operator, "req"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void attachmentOwnershipErrorsRejectUpload() throws Exception {
        UploadResult result = service.validateExcelUpload(file(1, "someone-elses-reference"), operator);
        assertThat(result.errors()).anyMatch(e -> e.errorCode().equals("UNOWNED_ATTACHMENT"));
        assertThat(result.savedCount()).isZero();
    }

    @Test
    void outsideEvaluationDateIsWarningNotSaveBlock() throws Exception {
        when(guards.countEvaluationDatePeriods(eq("2025"), eq(22L), any())).thenReturn(0);
        UploadResult result = service.validateExcelUpload(file(1, ""), operator);
        assertThat(result.validationStatus()).isEqualTo("VALIDATED");
        assertThat(result.warnings()).hasSize(1);
    }

    @Test
    void historyAndErrorReadsAreOwnerBound() {
        when(repository.ownedUpload("OTHER", 7L, false)).thenThrow(new NotFoundException("not found"));
        assertThatThrownBy(() -> service.listExcelErrors("OTHER", operator)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.downloadExcelErrors("OTHER", operator)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void targetReassignmentAndTemplateVersionChangeBlockCommit() throws Exception {
        stage(row);
        when(repository.targets("00123")).thenReturn(List.of(new Target(23L, "OTHER")));
        when(repository.inScope(eq(7L), any())).thenReturn(true);
        when(guards.countActiveInputPeriods("2025", 23L)).thenReturn(1);
        assertThatThrownBy(() -> service.commitExcelUpload("UPLOAD", operator, "req"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("TARGET_CHANGED");
        when(repository.currentTemplate()).thenReturn(new Template(template.templateId(), "2", template.columns()));
        assertThatThrownBy(() -> service.commitExcelUpload("UPLOAD", operator, "req"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("STALE_TEMPLATE");
    }

    @Test
    void guardLocksPrecedeRevalidationAndAchievementWrites() throws Exception {
        stage(row);
        service.commitExcelUpload("UPLOAD", operator, "request");
        var order = inOrder(repository);
        order.verify(repository).ownedUpload("UPLOAD", 7L, true);
        order.verify(repository).lockCommitGuards();
        order.verify(repository).currentTemplate();
        order.verify(repository).targets("00123");
        order.verify(repository).insertAchievement(target, row, LocalDate.parse("2025-04-10"),
                "[]", 7L, "request");
    }

    @Test
    void invalidDateRetainsIndependentNameAndAttachmentDiagnostics() throws Exception {
        byte[] bytes = workbook.template(template.templateId(), template.version(), template.columns());
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var data = book.getSheetAt(0).createRow(1);
            List<String> values = List.of("00123", "FR-032", "bad-date", "x".repeat(501), "unowned");
            for (int c = 0; c < values.size(); c++) data.createCell(c).setCellValue(values.get(c));
            var out = new ByteArrayOutputStream();
            book.write(out);
            UploadResult result = service.validateExcelUpload(new MockMultipartFile(
                    "file", "test.xlsx", EmploymentRateWorkbook.MIME, out.toByteArray()), operator);
            assertThat(result.errors()).extracting(ErrorRow::errorCode)
                    .containsExactly("INVALID_DATE", "VALUE_TOO_LONG", "UNOWNED_ATTACHMENT");
            assertThat(result.errorCount()).isEqualTo(1);
            assertThat(result.savedCount()).isZero();
        }
    }

    @Test
    void failedCommitDoesNotReplaceExistingValidationDiagnostics() {
        when(repository.errors("UPLOAD")).thenReturn(List.of(new ErrorRow(
                2, "실적명", "bad", "INVALID_VALUE", "invalid", "correct")));
        assertThatThrownBy(() -> service.commitExcelUpload("UPLOAD", operator, "request"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("UPLOAD_NOT_COMMITTABLE");
        verify(repository, never()).error(anyString(), any());
        verify(repository, never()).committed(anyString());
        verify(repository, never()).insertAchievement(any(), any(), any(), any(), anyLong(), any());
    }

    @Test
    void incompleteStagingCannotCommitOnlyNormalSubset() {
        when(repository.stagingComplete("UPLOAD")).thenReturn(false);
        assertThatThrownBy(() -> service.commitExcelUpload("UPLOAD", operator, "request"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("INCOMPLETE_STAGING");
        verify(repository, never()).insertAchievement(any(), any(), any(), any(), anyLong(), any());
        verify(repository, never()).committed(anyString());
    }

    @Test
    void malformedStagingCannotProducePartialWrites() {
        when(repository.staged("UPLOAD")).thenReturn(List.of("{}"));
        assertThatThrownBy(() -> service.commitExcelUpload("UPLOAD", operator, "request"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("INVALID_STAGING");
        verify(repository, never()).insertAchievement(any(), any(), any(), any(), anyLong(), any());
        verify(repository, never()).committed(anyString());
    }

    private void stage(InputRow input) throws Exception {
        when(repository.staged("UPLOAD")).thenReturn(List.of(json.writeValueAsString(
                new EmploymentRateExcelService.Staged(input, target, template.version()))));
    }

    private MockMultipartFile file(int count, String attachment) throws Exception {
        byte[] bytes = workbook.template(template.templateId(), template.version(), template.columns());
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            for (int i = 1; i <= count; i++) {
                var data = book.getSheetAt(0).createRow(i);
                List<String> values = List.of("00123", "FR-032", "2025-04-10", "Employment", attachment);
                for (int c = 0; c < values.size(); c++) data.createCell(c).setCellValue(values.get(c));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            book.write(out);
            return new MockMultipartFile("file", "test.xlsx", EmploymentRateWorkbook.MIME, out.toByteArray());
        }
    }

    private static CurrentUser user(String role) {
        return new CurrentUser(7L, "operator", "E7", "Operator", List.of(role), List.of());
    }
}
