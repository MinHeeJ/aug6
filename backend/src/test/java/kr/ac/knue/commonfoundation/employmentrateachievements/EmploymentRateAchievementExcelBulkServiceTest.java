package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorRow;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Covers the employment-rate adapter around the shared Excel boundary and the
 * deliberately policy-blocked R07 bulk operation.
 */
class EmploymentRateAchievementExcelBulkServiceTest {
    @Test
    void rejectedUploadKeepsTheSharedErrorArtifactAndReturnsAValidationFailure() {
        EmploymentRateAchievementMapper mapper = org.mockito.Mockito.mock(
                EmploymentRateAchievementMapper.class);
        ExcelOperationsService excelOperationsService = org.mockito.Mockito.mock(
                ExcelOperationsService.class);
        EmploymentRateAchievementService service = service(mapper, excelOperationsService);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employment-rate.csv",
                "text/csv",
                "교번,관리항목코드,업적발생일,실적명,첨부참조\nE9999,EMPLOYMENT,2026-10-04,취업률 실적,"
                        .getBytes(StandardCharsets.UTF_8));
        ExcelUploadResult rejected = new ExcelUploadResult(
                "UP-ERA-REJECTED",
                "EMPLOYMENT_RATE_ACHIEVEMENT",
                "employment-rate.csv",
                "REJECTED",
                1,
                0,
                1,
                0,
                0,
                List.of(new ExcelUploadErrorRow(
                        "ERR-ERA-001",
                        "UP-ERA-REJECTED",
                        2,
                        "교번",
                        "E9999",
                        "INVALID_CODE",
                        "존재하지 않는 교번입니다.",
                        "교번을 확인하세요.")));
        when(excelOperationsService.createExcelUpload(
                eq("EMPLOYMENT_RATE_ACHIEVEMENT"),
                eq("B83-EMPLOYMENT-RATE-ACHIEVEMENT-V1"),
                any(),
                eq(107L))).thenReturn(rejected);

        assertThatThrownBy(() -> service.upload(file, r07()))
                .isInstanceOf(BusinessValidationException.class)
                .satisfies(error -> assertThat(((BusinessValidationException) error).fields())
                        .extracting(field -> field.field())
                        .containsExactly("file"));

        verify(excelOperationsService).createExcelUpload(
                eq("EMPLOYMENT_RATE_ACHIEVEMENT"),
                eq("B83-EMPLOYMENT-RATE-ACHIEVEMENT-V1"),
                eq(file),
                eq(107L));
        verify(mapper, never()).insert(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void unapprovedBulkPolicyReturnsConflictWithoutPersistingAJob() {
        EmploymentRateAchievementMapper mapper = org.mockito.Mockito.mock(
                EmploymentRateAchievementMapper.class);
        EmploymentRateAchievementService service = service(
                mapper,
                org.mockito.Mockito.mock(ExcelOperationsService.class));

        assertThatThrownBy(() -> service.requestBatchJob(
                new EmploymentRateBulkJobRequest("2026", "GENERATE", null),
                r07()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("OQ-83-01");

        verify(mapper, never()).deleteBatchJob(any());
    }

    private EmploymentRateAchievementService service(
            EmploymentRateAchievementMapper mapper,
            ExcelOperationsService excelOperationsService) {
        return new EmploymentRateAchievementService(
                mapper,
                org.mockito.Mockito.mock(EducationAchievementGuardService.class),
                excelOperationsService,
                new ObjectMapper());
    }

    private CurrentUser r07() {
        return new CurrentUser(
                107L,
                "excel-operator",
                "E0107",
                "엑셀담당자",
                List.of("R07"),
                List.of());
    }
}
