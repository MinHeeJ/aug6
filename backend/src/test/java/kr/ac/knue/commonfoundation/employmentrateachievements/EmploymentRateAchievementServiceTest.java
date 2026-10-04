package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Focused feature tests for data scope, confirmed-lock, XLSX validation, and
 * neutral bulk-job retrieval in the employment-rate service.
 */
class EmploymentRateAchievementServiceTest {
    @Test
    void listUsesR01OwnershipScopeInsteadOfReturningEveryEmploymentRateRecord() {
        EmploymentRateAchievementMapper mapper = org.mockito.Mockito.mock(EmploymentRateAchievementMapper.class);
        EmploymentRateAchievementService service = service(mapper);
        CurrentUser r01 = user(101L, "R01");
        when(mapper.list(eq(20), eq(0), eq(101L), eq(true), eq(false), eq(false), eq(false)))
                .thenReturn(List.of(row(1L, 101L, "DRAFT")));
        when(mapper.count(eq(101L), eq(true), eq(false), eq(false), eq(false))).thenReturn(1L);

        EmploymentRateAchievementViews.SearchResponse result = service.list(0, 20, r01);

        assertThat(result.totalElements()).isEqualTo(1L);
        verify(mapper).list(eq(20), eq(0), eq(101L), eq(true), eq(false), eq(false), eq(false));
    }

    @Test
    void r07ListUsesOperationalAllScopeToSelectBulkJobTargets() {
        EmploymentRateAchievementMapper mapper = org.mockito.Mockito.mock(EmploymentRateAchievementMapper.class);
        EmploymentRateAchievementService service = service(mapper);
        CurrentUser r07 = user(107L, "R07");
        when(mapper.list(eq(20), eq(0), eq(107L), eq(false), eq(false), eq(false), eq(true)))
                .thenReturn(List.of(row(1L, 101L, "DRAFT")));
        when(mapper.count(eq(107L), eq(false), eq(false), eq(false), eq(true))).thenReturn(1L);

        EmploymentRateAchievementViews.SearchResponse result = service.list(0, 20, r07);

        assertThat(result.totalElements()).isEqualTo(1L);
        verify(mapper).list(eq(20), eq(0), eq(107L), eq(false), eq(false), eq(false), eq(true));
    }

    @Test
    void updateRejectsAnotherR01UsersRecordBeforeMutation() {
        EmploymentRateAchievementMapper mapper = org.mockito.Mockito.mock(EmploymentRateAchievementMapper.class);
        EmploymentRateAchievementService service = service(mapper);
        when(mapper.find(11L)).thenReturn(row(11L, 202L, "DRAFT"));
        EmploymentRateAchievementRequest request = request();

        assertThatThrownBy(() -> service.update(11L, request, user(101L, "R01"), "REQ-83-OWN"))
                .isInstanceOf(ForbiddenException.class);
        verify(mapper, never()).update(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateRetainsConfirmedRecordWithoutWriting() {
        EmploymentRateAchievementMapper mapper = org.mockito.Mockito.mock(EmploymentRateAchievementMapper.class);
        EmploymentRateAchievementService service = service(mapper);
        when(mapper.find(11L)).thenReturn(row(11L, 101L, "EVALUATION_CONFIRMED"));

        assertThatThrownBy(() -> service.update(11L, request(), user(101L, "R01"), "REQ-83-LOCK"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");
        verify(mapper, never()).update(any(), any(), any(), any(), any(), any());
    }

    @Test
    void invalidXlsxPreservesOnlyUploadErrorHistoryAndNoAchievementRows() throws Exception {
        EmploymentRateAchievementMapper mapper = org.mockito.Mockito.mock(EmploymentRateAchievementMapper.class);
        EmploymentRateAchievementService service = service(mapper);
        byte[] workbook = XlsxWorkbook.write(List.of(
                List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조"),
                List.of("E0101", "EMPLOYMENT_RATE", "not-a-date", "취업률", "file-ref")));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employment-rate.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbook);
        when(mapper.findActiveUserIdByEmployeeNo("E0101")).thenReturn(101L);

        EmploymentRateAchievementViews.UploadResult result = service.upload(file, user(107L, "R07"), "REQ-83-UPLOAD");

        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.errors()).extracting(EmploymentRateAchievementViews.UploadError::columnName)
                .containsExactly("achievementDate");
        verify(mapper).insertUpload(any(), any(), eq("employment-rate.xlsx"), eq("REJECTED"), eq(107L));
        verify(mapper).insertUploadHistory(any(), eq(1), eq(0), eq(1), eq(0), eq(107L));
        verify(mapper, never()).insert(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void bulkJobReadIsRestrictedToTheR07Requester() {
        EmploymentRateAchievementMapper mapper = org.mockito.Mockito.mock(EmploymentRateAchievementMapper.class);
        EmploymentRateAchievementService service = service(mapper);
        when(mapper.findBulkJob("B83-BATCH-001", 107L)).thenReturn(Map.of(
                "jobId", "B83-BATCH-001",
                "evaluationYear", "2026",
                "actionType", "GENERATE",
                "jobStatus", "QUEUED",
                "totalCount", 0,
                "processedCount", 0,
                "successCount", 0,
                "failureCount", 0,
                "requestedAt", LocalDateTime.parse("2026-01-01T09:00:00")));

        EmploymentRateAchievementViews.BulkJob job = service.getBulkJob("B83-BATCH-001", user(107L, "R07"));

        assertThat(job.jobId()).isEqualTo("B83-BATCH-001");
        verify(mapper).findBulkJob("B83-BATCH-001", 107L);
    }

    private EmploymentRateAchievementService service(EmploymentRateAchievementMapper mapper) {
        return new EmploymentRateAchievementService(
                mapper,
                org.mockito.Mockito.mock(EducationAchievementGuardService.class),
                new ObjectMapper());
    }

    private CurrentUser user(Long userId, String role) {
        return new CurrentUser(userId, "test-" + userId, "E" + userId, "테스트", List.of(role), List.of());
    }

    private EmploymentRateAchievementRequest request() {
        EmploymentRateAchievementRequest request = new EmploymentRateAchievementRequest();
        request.setManagementItemCode("EMPLOYMENT_RATE");
        request.setAchievementDate(LocalDate.parse("2026-04-01"));
        request.setAchievementName("취업률 실적");
        return request;
    }

    private Map<String, Object> row(Long id, Long ownerId, String status) {
        return Map.ofEntries(
                Map.entry("achievementId", id),
                Map.entry("managementNo", "ER-" + id),
                Map.entry("teacherUserId", ownerId),
                Map.entry("teacherName", "teacher"),
                Map.entry("evaluationYear", "2026"),
                Map.entry("managementItemCode", "EMPLOYMENT_RATE"),
                Map.entry("achievementDate", LocalDate.parse("2026-04-01")),
                Map.entry("achievementName", "취업률 실적"),
                Map.entry("achievementStatus", status),
                Map.entry("createdAt", LocalDateTime.parse("2026-04-01T09:00:00")),
                Map.entry("updatedAt", LocalDateTime.parse("2026-04-01T09:00:00")));
    }
}
