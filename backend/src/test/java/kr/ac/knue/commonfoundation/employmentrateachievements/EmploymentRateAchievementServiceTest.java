package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Service contract tests covering organization prerequisites, ownership and
 * confirmed locks, Excel all-or-nothing behavior, and persisted bulk results.
 */
class EmploymentRateAchievementServiceTest {
    private EmploymentRateAchievementMapper mapper;
    private EducationAchievementGuardService guardService;
    private ExcelOperationsService excelOperationsService;
    private EmploymentRateAchievementService service;
    private final CurrentUser r01 = new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(107L, "excel", "E0107", "담당자", List.of("R07"), List.of());

    @BeforeEach
    void setUp() {
        mapper = org.mockito.Mockito.mock(EmploymentRateAchievementMapper.class);
        guardService = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        excelOperationsService = org.mockito.Mockito.mock(ExcelOperationsService.class);
        service = new EmploymentRateAchievementService(
                mapper,
                guardService,
                excelOperationsService,
                new ObjectMapper());
    }

    @Test
    void createRequiresActiveOrganizationMappingBeforeInsert() {
        when(mapper.countActiveOrganizationMappings(101L)).thenReturn(0);

        assertThatThrownBy(() -> service.create(request(), r01, "REQ-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ACTIVE_ORGANIZATION_MAPPING_REQUIRED");

        verify(mapper, never()).insert(
                anyString(),
                anyLong(),
                anyString(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void updateRejectsAnotherUsersRowAndConfirmedRowsWithoutMutation() {
        when(mapper.find(501L)).thenReturn(row(202L, "DRAFT"));
        assertThatThrownBy(() -> service.update(501L, request(), r01, "REQ-2"))
                .isInstanceOf(ForbiddenException.class);
        verify(mapper, never()).update(anyLong(), anyLong(), any(), any(), any(), any(), any());

        when(mapper.find(502L)).thenReturn(row(101L, "EVALUATION_CONFIRMED"));
        when(mapper.countActiveOrganizationMappings(101L)).thenReturn(1);
        assertThatThrownBy(() -> service.update(502L, request(), r01, "REQ-3"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");
        verify(mapper, never()).update(anyLong(), anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void invalidExcelPreservesSharedUploadResultAndWritesNoBusinessRows() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.csv",
                "text/csv",
                "교번,관리항목코드,업적발생일,실적명\nE9999,EMPLOYMENT_RATE,2026-04-10,실적".getBytes());
        when(excelOperationsService.createExcelUpload(anyString(), any(), any(), anyLong()))
                .thenReturn(new ExcelUploadResult("UP-1", "EMPLOYMENT_RATE_ACHIEVEMENT", "invalid.csv",
                        "REJECTED", 1, 0, 1, 0, 0, List.of()));

        EmploymentRateExcelUploadResponse result = service.upload(file, r07, "REQ-UPLOAD");

        org.assertj.core.api.Assertions.assertThat(result.applied()).isFalse();
        org.assertj.core.api.Assertions.assertThat(result.errorFileToken()).isEqualTo("UP-1");
        verify(mapper, never()).insert(
                anyString(),
                anyLong(),
                anyString(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void confirmedBulkPreviewPersistsAndReturnsRetrievableTargetResults() {
        EmploymentRateBulkJobRequest request = new EmploymentRateBulkJobRequest(
                "2026",
                "GENERATE",
                java.util.Map.of("confirmed", true, "targetUserIds", List.of(101L, 202L)));
        when(mapper.findBulkJob(anyString(), eq(107L))).thenAnswer(invocation -> new EmploymentRateBulkJobRow(
                invocation.getArgument(0), "2026", "GENERATE", "REQUESTED", 2, 0, 2,
                LocalDateTime.parse("2026-04-10T09:00:00")));
        when(mapper.findBulkJobItems(anyString())).thenReturn(List.of(
                new EmploymentRateBulkJobItem(101L, null, false, "처리 대기"),
                new EmploymentRateBulkJobItem(202L, null, false, "처리 대기")));

        EmploymentRateBulkJobResponse created = service.createBulkJob(request, r07, "REQ-BULK");
        EmploymentRateBulkJobResponse found = service.getBulkJob(created.jobId(), r07);

        org.assertj.core.api.Assertions.assertThat(created.jobId()).startsWith("ERB-");
        org.assertj.core.api.Assertions.assertThat(found.items()).hasSize(2);
        verify(mapper).insertBulkJob(
                anyString(),
                eq("2026"),
                anyString(),
                eq("GENERATE"),
                eq(2),
                eq("REQ-BULK"),
                eq(107L));
        verify(mapper).insertBulkJobItem(created.jobId(), 101L, 107L);
        verify(mapper).insertBulkJobItem(created.jobId(), 202L, 107L);
    }

    private EmploymentRateAchievementRequest request() {
        return new EmploymentRateAchievementRequest(
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                "취업률 실적",
                List.of());
    }

    private EmploymentRateAchievementRow row(Long teacherUserId, String status) {
        return new EmploymentRateAchievementRow(
                501L,
                "ERA-001",
                teacherUserId,
                "faculty",
                "2026",
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                "취업률 실적",
                status,
                "[]",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}
