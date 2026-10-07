package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsMapper;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class EmploymentRateExcelServiceTest {
    private final ExcelOperationsMapper common = mock(ExcelOperationsMapper.class);
    private final EmploymentRateExcelMapper domain = mock(EmploymentRateExcelMapper.class);
    private final EducationAchievementGuardMapper guards = mock(EducationAchievementGuardMapper.class);
    private final EmploymentRateAchievementMapper achievements = mock(EmploymentRateAchievementMapper.class);
    private final EmploymentRateAchievementService authorization = mock(EmploymentRateAchievementService.class);
    private final EmploymentRateExcelService service = new EmploymentRateExcelService(
            common, domain, guards, new ObjectMapper(), achievements, authorization);
    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "관리자", List.of("R09"), List.of());

    @BeforeEach
    void activeTemplate() {
        when(domain.findTemplateId()).thenReturn("EMPLOYMENT-RATE-TEMPLATE-001");
        List<String> refs = List.of("users.employee_no",
                "evaluation_element_management_item_settings.management_item_code",
                "ISO_DATE", "OPTIONAL_TEXT", "OPTIONAL_OWNED_FILE_TOKEN");
        var rules = java.util.stream.IntStream.range(0, 5)
                .mapToObj(index -> new kr.ac.knue.commonfoundation.excel.ExcelTemplateRuleRow(
                        "RULE-" + index, EmploymentRateExcelService.HEADERS.get(index), index + 1, refs.get(index)))
                .toList();
        when(common.listTemplateRules("EMPLOYMENT-RATE-TEMPLATE-001")).thenReturn(rules);
    }

    @Test
    void validXlsxStagesActualPayloadAndNeverClaimsSavedDomainRows() throws Exception {
        when(domain.findActiveTeacher("E1001")).thenReturn(2L);
        when(domain.countActiveItems("EDU001")).thenReturn(1);
        when(guards.countActiveInputPeriods("2026", 2L)).thenReturn(1);
        ExcelUploadResult result = (ExcelUploadResult) service.upload(file("E1001", "2026-04-10"), admin, "req-1");
        assertThat(result.validationStatus()).isEqualTo("VALIDATED");
        assertThat(result.savedCount()).isZero();
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(common).insertStagingRow(anyString(), eq(result.uploadId()), eq(2), payload.capture(), eq("NORMAL"));
        var json = new ObjectMapper().readTree(payload.getValue());
        assertThat(json.path("teacherUserId").asLong()).isEqualTo(2L);
        assertThat(json.path("achievementDate").asText()).isEqualTo("2026-04-10");
        assertThat(json.path("requestId").asText()).isEqualTo("req-1");
        verify(common, never()).markUploadCommitted(anyString());
        verify(common, never()).deleteNormalStagingRows(anyString());
    }

    @Test
    void malformedWorkbookRetainsDiagnosticAndReturnsValidationFailure() {
        var file = new MockMultipartFile("file", "bad.xlsx", "application/octet-stream", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.upload(file, admin, "req-2"))
                .isInstanceOf(BusinessValidationException.class);
        verify(common).insertUploadError(argThat(error -> error.errorCode().equals("INVALID_WORKBOOK")));
        verify(common).insertUploadFile(anyString(), eq("EMPLOYMENT_RATE_ACHIEVEMENT"), isNull(),
                anyString(), eq("bad.xlsx"), eq(1L), eq("REJECTED"));
        verifyNoInteractions(domain, guards);
    }

    @Test
    void r07CannotStageAnotherOrganizationsTeacher() {
        when(domain.findActiveTeacher("E1001")).thenReturn(2L);
        when(domain.countActiveItems("EDU001")).thenReturn(1);
        var uploader = new CurrentUser(3L, "staff", "E3001", "직원", List.of("R07"), List.of());
        ExcelUploadResult result = service.upload(file("E1001", "2026-04-10"), uploader, "req-3");
        assertThat(result.validationStatus()).isEqualTo("ERROR");
        assertThat(result.savedCount()).isZero();
        verify(guards).countCertificationScope(3L, 2L);
        verify(guards, never()).countSharedActiveOrganization(anyLong(), anyLong());
        verify(common).insertUploadError(argThat(error -> error.errorCode().equals("OUT_OF_SCOPE")));
        verify(guards, never()).countActiveInputPeriods(anyString(), anyLong());
    }

    @Test
    void deniesNonUploadRoleBeforeReadingOrPersisting() {
        var professor = new CurrentUser(2L, "professor", "E1001", "교수", List.of("R01"), List.of());
        assertThatThrownBy(() -> service.upload(file("E1001", "2026-04-10"), professor, "req-4"))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(common, domain, guards);
    }

    @Test
    void rejectsDuplicateRowsAndRetainsEveryRealPayload() {
        when(domain.findActiveTeacher("E1001")).thenReturn(2L);
        when(domain.countActiveItems("EDU001")).thenReturn(1);
        when(guards.countActiveInputPeriods("2026", 2L)).thenReturn(1);
        byte[] bytes = EmploymentRateWorkbook.write(List.of(
                EmploymentRateExcelService.HEADERS,
                List.of("E1001", "EDU001", "2026-04-10", "동일 실적", ""),
                List.of("E1001", "EDU001", "2026-04-10", "동일 실적", "")));
        var upload = new MockMultipartFile("file", "duplicates.xlsx", "application/octet-stream", bytes);
        ExcelUploadResult result = service.upload(upload, admin, "duplicate-request");
        assertThat(result.validationStatus()).isEqualTo("ERROR");
        assertThat(result.savedCount()).isZero();
        verify(common).insertUploadError(argThat(error ->
                error.rowNumber() == 3 && error.errorCode().equals("DUPLICATE")));
        verify(common).insertStagingRow(anyString(), anyString(), eq(2), contains("동일 실적"), eq("NORMAL"));
        verify(common).insertStagingRow(anyString(), anyString(), eq(3), contains("동일 실적"), eq("ERROR"));
        verify(common).upsertUploadHistory(anyString(), eq(2), eq(1), eq(1), eq(0), eq(0), anyLong(), eq(1L));
    }

    @Test
    void genericCommitCannotDestroyEmploymentStagingOrClaimSuccess() {
        var generic = new kr.ac.knue.commonfoundation.excel.ExcelOperationsService(common);
        when(common.findUploadBusinessType("ER-UP-example")).thenReturn("EMPLOYMENT_RATE_ACHIEVEMENT");
        assertThatThrownBy(() -> generic.commitExcelUpload("ER-UP-example", admin.userId()))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ConflictException.class);
        verify(common, never()).markUploadCommitted(anyString());
        verify(common, never()).deleteNormalStagingRows(anyString());
    }

    @Test
    void genericUploadCannotBypassTheActualEmploymentParser() {
        var generic = new kr.ac.knue.commonfoundation.excel.ExcelOperationsService(common);
        assertThatThrownBy(() -> generic.createExcelUpload(
                "EMPLOYMENT_RATE_ACHIEVEMENT", null, file("E1001", "2026-04-10"), 1L))
                .isInstanceOf(BusinessValidationException.class);
        verifyNoInteractions(common);
    }

    @Test
    void downloadIsAnActualReuploadableWorkbook() {
        var bytes = service.download(List.of(Map.of(
                "employeeNo", "E1001", "managementItemCode", "EDU001",
                "achievementDate", "2026-04-10", "achievementName", "실적")));
        assertThat(EmploymentRateWorkbook.read(bytes).get(1).cells())
                .containsExactly("E1001", "EDU001", "2026-04-10", "실적", "");
    }

    @Test
    void commitUsesGeneratedKeyAuditsAndCompletesOnlyAfterMaterialization() throws Exception {
        stageForCommit();
        when(achievements.insert(anyMap())).thenAnswer(invocation -> {
            Map<String, Object> row = invocation.getArgument(0);
            row.put("achievementId", 77L);
            return 1;
        });
        assertThat(service.commit("ER-UP-test", admin, "commit-1").savedCount()).isEqualTo(1);
        verify(domain).findUpload("ER-UP-test", true);
        verify(achievements).history(argThat(row -> "77".equals(row.get("targetKey"))
                && "commit-1".equals(row.get("requestId")) && "teacherUserId".equals(row.get("fieldName"))));
        var order = inOrder(achievements, common);
        order.verify(achievements).insert(anyMap());
        order.verify(achievements, atLeastOnce()).history(anyMap());
        order.verify(common).markUploadCommitted("ER-UP-test");
        verify(common).deleteNormalStagingRows("ER-UP-test");
    }

    @Test
    void lastRowRevalidationFailureWritesNoAchievementsAndRetainsDiagnostics() throws Exception {
        stageForCommit();
        when(domain.stagedRows("ER-UP-test")).thenReturn(List.of(
                staged(2, "2026-04-10"), staged(3, "2026-04-11")));
        when(domain.countDuplicates(2L, "2026", "EDU001", "2026-04-11", "실적")).thenReturn(1);
        assertThatThrownBy(() -> service.commit("ER-UP-test", admin, "commit-2"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ConflictException.class);
        verify(achievements, never()).insert(anyMap());
        verify(achievements, never()).history(anyMap());
        verify(common).insertUploadError(argThat(error -> error.rowNumber() == 3));
        verify(common).upsertUploadHistory(eq("ER-UP-test"), eq(2), eq(1), eq(1), eq(0), eq(0), anyLong(), eq(1L));
        verify(common, never()).deleteNormalStagingRows(anyString());
        verify(common, never()).markUploadCommitted(anyString());
    }

    @Test
    void commitRejectsReplayAndWrongOwnerBeforeDomainWrites() {
        when(domain.findUpload("ER-UP-test", true)).thenReturn(Map.of(
                "uploaderUserId", 1L, "validationStatus", "COMMITTED"));
        assertThatThrownBy(() -> service.commit("ER-UP-test", admin, "commit-3"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ConflictException.class);
        var staff = new CurrentUser(3L, "staff", "E3001", "직원", List.of("R07"), List.of());
        assertThatThrownBy(() -> service.commit("ER-UP-test", staff, "commit-4"))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(achievements);
        verify(common, never()).markUploadCommitted(anyString());
    }

    private void stageForCommit() throws Exception {
        when(achievements.history(anyMap())).thenReturn(1);
        when(domain.findUpload("ER-UP-test", true)).thenReturn(Map.of(
                "uploaderUserId", 1L, "validationStatus", "VALIDATED"));
        when(domain.stagedRows("ER-UP-test")).thenReturn(List.of(staged(2, "2026-04-10")));
        when(domain.findActiveTeacher("E1001")).thenReturn(2L);
        when(domain.countActiveItems("EDU001")).thenReturn(1);
        when(guards.countActiveInputPeriods("2026", 2L)).thenReturn(1);
        when(achievements.organizations(eq(2L), any())).thenReturn(List.of("ORG-1"));
    }

    private Map<String, Object> staged(int row, String date) throws Exception {
        return Map.of("rowNumber", row, "payload", new ObjectMapper().writeValueAsString(Map.of(
                "rawCells", List.of("E1001", "EDU001", date, "실적", ""))));
    }

    @Test
    void r07SelfAndSharedOrganizationNeverBypassCertificationMapping() {
        when(domain.findActiveTeacher("E1001")).thenReturn(3L);
        when(domain.countActiveItems("EDU001")).thenReturn(1);
        when(guards.countSharedActiveOrganization(3L, 3L)).thenReturn(1);
        var staff = new CurrentUser(3L, "staff", "E3001", "직원", List.of("R07"), List.of());
        assertThat(service.upload(file("E1001", "2026-04-10"), staff, "scope-self").validationStatus())
                .isEqualTo("ERROR");
        verify(guards).countCertificationScope(3L, 3L);
        verify(guards, never()).countSharedActiveOrganization(anyLong(), anyLong());
    }

    @Test
    void validationConflictCommitsDiagnosticsButWriteFailureRollsBackThroughSpringProxy() throws Exception {
        stageForCommit();
        var transactions = new TrackingTransactions();
        var factory = new org.springframework.aop.framework.ProxyFactory(service);
        factory.setProxyTargetClass(true);
        var interceptor = new org.springframework.transaction.interceptor.TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource());
        factory.addAdvice(interceptor);
        var proxy = (EmploymentRateExcelService) factory.getProxy();
        when(domain.countDuplicates(2L, "2026", "EDU001", "2026-04-10", "실적")).thenReturn(1);
        assertThatThrownBy(() -> proxy.commit("ER-UP-test", admin, "txn-diagnostics"))
                .isInstanceOf(EmploymentRateExcelService.RetainedValidationConflict.class);
        assertThat(transactions.commits).isEqualTo(1);
        assertThat(transactions.rollbacks).isZero();
        when(domain.countDuplicates(2L, "2026", "EDU001", "2026-04-10", "실적")).thenReturn(0);
        when(domain.stagedRows("ER-UP-test")).thenReturn(List.of(
                staged(2, "2026-04-10"), staged(3, "2026-04-11")));
        var inserted = new java.util.concurrent.atomic.AtomicInteger();
        when(achievements.insert(anyMap())).thenAnswer(call -> {
            if (inserted.incrementAndGet() == 2) throw new IllegalStateException("second database write failed");
            Map<String, Object> row = call.getArgument(0);
            row.put("achievementId", 77L);
            return 1;
        });
        assertThatThrownBy(() -> proxy.commit("ER-UP-test", admin, "txn-failure"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(transactions.commits).isEqualTo(1);
        assertThat(transactions.rollbacks).isEqualTo(1);
        verify(common, never()).markUploadCommitted(anyString());
        verify(common, never()).deleteNormalStagingRows(anyString());
    }

    private static final class TrackingTransactions
            extends org.springframework.transaction.support.AbstractPlatformTransactionManager {
        int commits;
        int rollbacks;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) { }
        @Override protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) { rollbacks++; }
    }

    private MockMultipartFile file(String teacher, String date) {
        byte[] bytes = EmploymentRateWorkbook.write(List.of(
                EmploymentRateExcelService.HEADERS,
                List.of(teacher, "EDU001", date, "실적", "")));
        return new MockMultipartFile("file", "upload.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
    }
}
