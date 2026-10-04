package kr.ac.knue.commonfoundation.lectureimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies service-level generated-key propagation, ownership, lifecycle, and
 * scoped read behavior before the mapper is exercised against PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class LectureImprovementServiceTest {
    @Mock
    private LectureImprovementMapper mapper;

    @Mock
    private EducationAchievementGuardService guardService;

    private LectureImprovementService service;
    private final CurrentUser r01 = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());

    @BeforeEach
    void setUp() {
        service = new LectureImprovementService(mapper, guardService, new ObjectMapper());
    }

    @Test
    void createUsesGeneratedHeaderIdForDetailReadBackAndAttachmentJson() {
        LectureImprovementSaveRequest request = request("2025-09-01", List.of("file-a", "file-b"));
        doAnswer(invocation -> {
            invocation.<LectureImprovementInsertCommand>getArgument(0).setAchievementId(124L);
            return null;
        }).when(mapper).insertAchievement(any(LectureImprovementInsertCommand.class));
        when(guardService.validateMutation(any(), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.findVisible(eq(124L), eq(101L), eq(List.of("R01")))).thenReturn(row(124L, "2025"));

        LectureImprovementSaveResponse response = service.create(request, r01, "REQ-LI-CREATE");

        ArgumentCaptor<LectureImprovementInsertCommand> command = ArgumentCaptor.forClass(
                LectureImprovementInsertCommand.class);
        verify(mapper).insertAchievement(command.capture());
        verify(mapper).insertDetail(124L, "강의 피드백 개선", 2025, 2);
        assertThat(command.getValue().getAchievementId()).isEqualTo(124L);
        assertThat(command.getValue().getAttachmentIds()).isEqualTo("[\"file-a\",\"file-b\"]");
        assertThat(response.achievement().achievementId()).isEqualTo(124L);
        assertThat(response.achievement().attachmentIds()).containsExactly("file-a", "file-b");
    }

    @Test
    void r01CreateUsesTheAuthenticatedFacultyAsTheAchievementOwner() {
        LectureImprovementSaveRequest request = request("2025-09-01", List.of());
        doAnswer(invocation -> {
            invocation.<LectureImprovementInsertCommand>getArgument(0).setAchievementId(125L);
            return null;
        }).when(mapper).insertAchievement(any(LectureImprovementInsertCommand.class));
        when(guardService.validateMutation(same(r01), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.findVisible(eq(125L), eq(101L), eq(List.of("R01")))).thenReturn(row(125L, "2025"));

        service.create(request, r01, "REQ-LI-OWNER");

        ArgumentCaptor<LectureImprovementInsertCommand> command = ArgumentCaptor.forClass(
                LectureImprovementInsertCommand.class);
        verify(mapper).insertAchievement(command.capture());
        assertThat(command.getValue().getTeacherUserId()).isEqualTo(r01.userId());
        assertThat(command.getValue().getCreatedBy()).isEqualTo(r01.userId());
    }

    @Test
    void r02AndR04ReadsReachTheMapperWithTheirOwnDataScopes() {
        CurrentUser r02 = userWithRole(202L, "R02");
        CurrentUser r04 = userWithRole(404L, "R04");
        when(mapper.findVisible(eq(83L), eq(202L), eq(List.of("R02")))).thenReturn(row(83L, "2025"));
        when(mapper.findVisible(eq(83L), eq(404L), eq(List.of("R04")))).thenReturn(row(83L, "2025"));

        assertThat(service.get(83L, r02).achievementId()).isEqualTo(83L);
        assertThat(service.get(83L, r04).achievementId()).isEqualTo(83L);

        verify(mapper).findVisible(83L, 202L, List.of("R02"));
        verify(mapper).findVisible(83L, 404L, List.of("R04"));
    }

    @Test
    void updateRejectsAnEvaluationConfirmedRecordBeforeMapperMutation() {
        LectureImprovementSaveRequest request = request("2025-09-02", List.of());
        when(mapper.findVisible(eq(83L), eq(101L), eq(List.of("R01")))).thenReturn(row(83L, "2025"));
        when(guardService.validateMutation(any(), any())).thenThrow(
                new ConflictException("CONFIRMED_DATA_LOCKED"));

        assertThatThrownBy(() -> service.update(83L, request, r01, "REQ-LI-LOCK"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).updateAchievement(any(), any(), any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any(), any(), any());
    }

    @Test
    void updateRejectsCrossEvaluationYearDateBeforeGuardOrMapperMutation() {
        LectureImprovementSaveRequest request = request("2026-01-01", List.of());
        when(mapper.findVisible(eq(83L), eq(101L), eq(List.of("R01")))).thenReturn(row(83L, "2025"));

        assertThatThrownBy(() -> service.update(83L, request, r01, "REQ-LI-YEAR"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("EVALUATION_YEAR_CHANGE_NOT_ALLOWED");

        verify(guardService, never()).validateMutation(any(), any());
        verify(mapper, never()).updateAchievement(any(), any(), any(), any(), any());
    }

    @Test
    void updateRecordsBeforeAndAfterValuesForEveryChangedPersistedField() {
        LectureImprovementRow existing = row(
                83L,
                "OLD_MANAGEMENT_ITEM",
                "2025-09-01",
                "기존 개선 내용",
                2024,
                1,
                List.of("file-old"));
        LectureImprovementRow saved = row(
                83L,
                "NEW_MANAGEMENT_ITEM",
                "2025-09-02",
                "변경된 개선 내용",
                2025,
                2,
                List.of("file-new-a", "file-new-b"));
        LectureImprovementSaveRequest request = new LectureImprovementSaveRequest(
                "NEW_MANAGEMENT_ITEM",
                LocalDate.parse("2025-09-02"),
                "변경된 개선 내용",
                2025,
                "2",
                List.of("file-new-a", "file-new-b"));
        when(mapper.findVisible(eq(83L), eq(101L), eq(List.of("R01"))))
                .thenReturn(existing, saved);
        when(guardService.validateMutation(same(r01), any())).thenReturn(OccurredDateValidation.accepted());

        service.update(83L, request, r01, "REQ-LI-AUDIT");

        verify(mapper, times(6)).insertChangeHistory(
                eq("education_achievements"),
                eq("83"),
                eq("UPDATE"),
                any(),
                any(),
                any(),
                eq(101L),
                eq("강의개선 실적 수정 (REQ-LI-AUDIT)"));
        verifyChangeHistory(
                "managementItemCode",
                "OLD_MANAGEMENT_ITEM",
                "NEW_MANAGEMENT_ITEM");
        verifyChangeHistory("achievementDate", "2025-09-01", "2025-09-02");
        verifyChangeHistory("achievementContent", "기존 개선 내용", "변경된 개선 내용");
        verifyChangeHistory("academicYear", "2024", "2025");
        verifyChangeHistory("semesterCode", "1", "2");
        verifyChangeHistory(
                "attachmentIds",
                "[\"file-old\"]",
                "[\"file-new-a\",\"file-new-b\"]");
    }

    private void verifyChangeHistory(String fieldName, String beforeValue, String afterValue) {
        verify(mapper).insertChangeHistory(
                "education_achievements",
                "83",
                "UPDATE",
                fieldName,
                beforeValue,
                afterValue,
                101L,
                "강의개선 실적 수정 (REQ-LI-AUDIT)");
    }

    private LectureImprovementSaveRequest request(String achievementDate, List<String> attachmentIds) {
        return new LectureImprovementSaveRequest(
                "LECTURE_IMPROVEMENT",
                LocalDate.parse(achievementDate),
                "강의 피드백 개선",
                2025,
                "2",
                attachmentIds);
    }

    private CurrentUser userWithRole(Long userId, String role) {
        return new CurrentUser(userId, "user" + userId, "E" + userId, "사용자", List.of(role), List.of());
    }

    private LectureImprovementRow row(Long achievementId, String evaluationYear) {
        return row(
                achievementId,
                "LECTURE_IMPROVEMENT",
                evaluationYear + "-09-01",
                "강의 피드백 개선",
                Integer.parseInt(evaluationYear),
                2,
                List.of("file-a", "file-b"));
    }

    private LectureImprovementRow row(
            Long achievementId,
            String managementItemCode,
            String achievementDate,
            String achievementContent,
            Integer academicYear,
            Integer semester,
            List<String> attachmentIds) {
        LocalDateTime timestamp = LocalDateTime.parse("2025-09-01T09:00:00");
        return new LectureImprovementRow(
                achievementId,
                "B83-LI-" + achievementId,
                101L,
                "faculty",
                "2025",
                managementItemCode,
                LocalDate.parse(achievementDate),
                achievementContent,
                academicYear,
                semester,
                attachmentIds,
                "DRAFT",
                timestamp,
                timestamp);
    }
}
