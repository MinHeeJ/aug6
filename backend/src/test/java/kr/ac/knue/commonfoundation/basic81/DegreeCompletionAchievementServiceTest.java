package kr.ac.knue.commonfoundation.basic81;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

/** Verifies the degree header and student details stay in one guarded transaction. */
class DegreeCompletionAchievementServiceTest {
    @Test
    void saveCreatesHeaderStudentsStatusAndDataChangeHistory() {
        DegreeCompletionAchievementMapper mapper = org.mockito.Mockito.mock(
                DegreeCompletionAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        DegreeCompletionAchievementService service = new DegreeCompletionAchievementService(
                mapper,
                guardService);
        CurrentUser requester = r01();
        SaveDegreeCompletionAchievementRequest request = request(null, "MASTER");
        DegreeCompletionAchievementHeaderRow saved = header(101L, "DC-save-test");
        when(mapper.findHeaderByManagementNo(any())).thenReturn(saved);
        when(mapper.findHeader(101L)).thenReturn(saved);
        when(mapper.findStudents(101L)).thenReturn(List.of(student(1001L, "MASTER")));

        DegreeCompletionAchievementRow result = service.save(request, requester, "REQ-B81-DC-SAVE");

        assertThat(result.students()).extracting(DegreeCompletionStudent::degreeType).containsExactly("MASTER");
        verify(mapper).insertHeader(
                any(),
                eq(101L),
                eq("2026"),
                eq("DEGREE_COMPLETION"),
                eq(LocalDate.parse("2026-02-20")),
                eq("학생 수: 1"),
                eq("attachment-opaque-ref"),
                eq(101L));
        verify(mapper).insertStudent(eq(101L), eq(request.students().get(0)), eq(101L));
        verify(mapper).insertStatusHistory(any(EducationAchievementStatusHistory.class));
        verify(mapper).insertChangeHistory(
                eq("degree_completion_achievements"),
                eq("101"),
                eq("CREATE"),
                eq("student_count"),
                eq(null),
                eq("1"),
                eq(101L),
                eq("석·박사 배출 실적 저장"),
                eq("REQ-B81-DC-SAVE"));
    }

    @Test
    void saveRejectsConfirmedDegreeHeaderBeforeStudentReplacement() {
        DegreeCompletionAchievementMapper mapper = org.mockito.Mockito.mock(
                DegreeCompletionAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        DegreeCompletionAchievementService service = new DegreeCompletionAchievementService(
                mapper,
                guardService);
        SaveDegreeCompletionAchievementRequest request = request(101L, "DOCTORAL");
        when(mapper.findHeader(101L)).thenReturn(header(101L, "B77-DC-001"));
        when(guardService.validateMutation(eq(r01()), any())).thenThrow(
                new ConflictException("CONFIRMED_DATA_LOCKED"));

        assertThatThrownBy(() -> service.save(request, r01(), "REQ-B81-DC-CONFLICT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).updateHeader(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).deleteStudents(any());
        verify(mapper, never()).insertStudent(any(), any(), any());
    }

    private CurrentUser r01() {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    }

    private SaveDegreeCompletionAchievementRequest request(Long achievementId, String degreeType) {
        return new SaveDegreeCompletionAchievementRequest(
                achievementId,
                "DEGREE_COMPLETION",
                "attachment-opaque-ref",
                List.of(new DegreeCompletionStudentRequest(
                        degreeType,
                        "홍길동",
                        "교육과정 연구",
                        LocalDate.parse("2026-02-20"))));
    }

    private DegreeCompletionAchievementHeaderRow header(Long id, String managementNo) {
        return new DegreeCompletionAchievementHeaderRow(
                id,
                managementNo,
                101L,
                "교원",
                "2026",
                "DEGREE_COMPLETION",
                LocalDate.parse("2026-02-20"),
                "학생 수: 1",
                "DRAFT",
                "attachment-opaque-ref",
                LocalDateTime.parse("2026-02-20T09:00:00"),
                LocalDateTime.parse("2026-02-20T09:00:00"));
    }

    private DegreeCompletionStudent student(Long id, String degreeType) {
        return new DegreeCompletionStudent(
                id,
                101L,
                degreeType,
                "홍길동",
                "교육과정 연구",
                LocalDate.parse("2026-02-20"));
    }
}
