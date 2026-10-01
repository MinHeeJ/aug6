package kr.ac.knue.commonfoundation.basic81;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** Verifies student-guidance write and R07 Excel all-or-nothing service behavior. */
class StudentGuidanceAchievementServiceTest {
    @Test
    void saveCreatesHeaderStudentsStatusAndChangeHistory() {
        StudentGuidanceAchievementMapper mapper = org.mockito.Mockito.mock(StudentGuidanceAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        StudentGuidanceAchievementService service = new StudentGuidanceAchievementService(
                mapper,
                guardService,
                new ObjectMapper(),
                org.mockito.Mockito.mock(kr.ac.knue.commonfoundation.excel.ExcelOperationsService.class));
        CurrentUser requester = r01();
        SaveStudentGuidanceAchievementRequest request = new SaveStudentGuidanceAchievementRequest(
                null,
                "STUDENT_GUIDANCE",
                LocalDate.parse("2026-03-01"),
                LocalDate.parse("2026-03-31"),
                "attachment-opaque-ref",
                List.of(new ObjectMapper().createObjectNode().put("studentName", "홍길동")));
        StudentGuidanceAchievementRow saved = row(91L, "SG-save-test", 1);
        when(guardService.validateMutation(eq(requester), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.findByManagementNo(any())).thenReturn(saved);
        when(mapper.find(91L)).thenReturn(saved);

        StudentGuidanceAchievementRow result = service.save(request, requester, "REQ-B81-SG-SAVE");

        assertThat(result.managementItemCode()).isEqualTo("STUDENT_GUIDANCE");
        verify(mapper).insertHeader(
                any(),
                eq(101L),
                eq("2026"),
                eq("STUDENT_GUIDANCE"),
                eq(LocalDate.parse("2026-03-01")),
                eq(LocalDate.parse("2026-03-31")),
                eq(1),
                eq("attachment-opaque-ref"),
                eq(101L));
        verify(mapper).insertStudent(eq(91L), eq("{\"studentName\":\"홍길동\"}"), eq(101L));
        verify(mapper).insertStatusHistory(any(EducationAchievementStatusHistory.class));
        verify(mapper).insertChangeHistory(
                eq("student_guidance_achievements"),
                eq("91"),
                eq("CREATE"),
                eq("student_count"),
                eq(null),
                eq("1"),
                eq(101L),
                eq("학생지도 실적 저장"),
                eq("REQ-B81-SG-SAVE"));
    }

    @Test
    void duplicateExcelRowsAreRejectedAndCannotCommitDomainAchievements() {
        StudentGuidanceAchievementMapper mapper = org.mockito.Mockito.mock(StudentGuidanceAchievementMapper.class);
        StudentGuidanceAchievementService service = new StudentGuidanceAchievementService(
                mapper,
                org.mockito.Mockito.mock(EducationAchievementGuardService.class),
                new ObjectMapper(),
                org.mockito.Mockito.mock(ExcelOperationsService.class));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "student-guidance.csv",
                "text/csv",
                ("templateVersion,managementItemCode,guidanceStartDate,guidanceEndDate,studentName\n"
                        + "v1.0,STUDENT_GUIDANCE,2026-03-01,2026-03-31,홍길동\n"
                        + "v1.0,STUDENT_GUIDANCE,2026-03-01,2026-03-31,홍길동")
                        .getBytes(StandardCharsets.UTF_8));

        StudentGuidanceExcelUploadResult result = service.validateExcelUpload(file, r07());

        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.errors()).singleElement().extracting(StudentGuidanceExcelErrorRow::errorCode)
                .isEqualTo("DUPLICATE");
        verify(mapper).insertExcelError(any(), eq(result.uploadId()), eq(3), eq("studentName"), eq("홍길동"),
                eq("DUPLICATE"), any(), any());
        when(mapper.existsExcelUpload(result.uploadId())).thenReturn(1);
        when(mapper.countExcelErrors(result.uploadId())).thenReturn(1);

        assertThatThrownBy(() -> service.commitExcelUpload(result.uploadId(), r07(), "REQ-B81-SG-COMMIT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("전체 반영을 차단");

        verify(mapper, never()).insertHeader(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    }

    private CurrentUser r01() {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    }

    private CurrentUser r07() {
        return new CurrentUser(107L, "excel-operator", "E0107", "엑셀담당자", List.of("R07"), List.of());
    }

    private StudentGuidanceAchievementRow row(Long id, String managementNo, int studentCount) {
        return new StudentGuidanceAchievementRow(
                id,
                managementNo,
                101L,
                "faculty",
                "2026",
                "STUDENT_GUIDANCE",
                LocalDate.parse("2026-03-01"),
                LocalDate.parse("2026-03-31"),
                studentCount,
                "DRAFT",
                "attachment-opaque-ref",
                "[{\"studentName\":\"홍길동\"}]",
                LocalDateTime.parse("2026-03-01T09:00:00"),
                LocalDateTime.parse("2026-03-01T09:00:00"));
    }
}
