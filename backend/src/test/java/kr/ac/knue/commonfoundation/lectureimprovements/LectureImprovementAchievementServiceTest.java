package kr.ac.knue.commonfoundation.lectureimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.CreateCommand;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.Request;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.Row;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.SearchCriteria;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Verifies lecture-improvement write commands retain the required lifecycle and audit side effects. */
class LectureImprovementAchievementServiceTest {
    @Test
    void listPassesR02ToTheScopedMapperInsteadOfLoadingUnscopedRows() {
        LectureImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureImprovementAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        LectureImprovementAchievementService service = new LectureImprovementAchievementService(
                mapper,
                guardService,
                new ObjectMapper());
        CurrentUser requester = new CurrentUser(
                102L, "department-head", "E0102", "학과장", List.of("R02"), List.of());
        SearchCriteria criteria = new SearchCriteria(0, 20);
        when(mapper.list(criteria, 102L, requester.roles())).thenReturn(List.of(row("DRAFT", "범위 내 내용")));
        when(mapper.count(criteria, 102L, requester.roles())).thenReturn(1L);

        var result = service.list(criteria, requester);

        assertThat(result.achievements()).hasSize(1);
        verify(mapper).list(criteria, 102L, requester.roles());
        verify(mapper).count(criteria, 102L, requester.roles());
        verify(mapper, never()).findById(any());
    }

    @Test
    void createPersistsHeaderDetailStatusAndChangeHistoryTogether() {
        LectureImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureImprovementAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        LectureImprovementAchievementService service = new LectureImprovementAchievementService(
                mapper,
                guardService,
                new ObjectMapper());
        CurrentUser requester = new CurrentUser(
                101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
        Request request = new Request(
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-09-01"),
                "토론형 수업 개선",
                2025,
                2,
                List.of("attachment-opaque-ref"));
        Row saved = row("DRAFT", "토론형 수업 개선");
        when(guardService.validateMutation(eq(requester), any()))
                .thenReturn(OccurredDateValidation.accepted());
        when(mapper.findOrganizationCode(101L)).thenReturn("KNUE-DEPT-COMP");
        doAnswer(invocation -> {
            CreateCommand command = invocation.getArgument(0);
            command.setAchievementId(83L);
            return null;
        }).when(mapper).insertAchievement(any(CreateCommand.class));
        when(mapper.findById(83L)).thenReturn(saved);

        var result = service.create(request, requester, "REQ-B83-LI-CREATE");

        assertThat(result.achievement().academicYear()).isEqualTo(2025);
        assertThat(result.achievement().semester()).isEqualTo(2);
        ArgumentCaptor<CreateCommand> commandCaptor = ArgumentCaptor.forClass(CreateCommand.class);
        verify(mapper).insertAchievement(commandCaptor.capture());
        CreateCommand persistedCommand = commandCaptor.getValue();
        assertThat(persistedCommand.getAchievementId()).isEqualTo(83L);
        assertThat(persistedCommand.getTeacherUserId()).isEqualTo(101L);
        assertThat(persistedCommand.getOrganizationCode()).isEqualTo("KNUE-DEPT-COMP");
        assertThat(persistedCommand.getEvaluationYear()).isEqualTo("2025");
        assertThat(persistedCommand.getManagementItemCode()).isEqualTo("LECTURE_IMPROVEMENT");
        assertThat(persistedCommand.getAchievementDate()).isEqualTo(LocalDate.parse("2025-09-01"));
        assertThat(persistedCommand.getAchievementName()).isEqualTo("2025학년도 2학기 강의개선");
        assertThat(persistedCommand.getAttachmentIds()).isEqualTo("[\"attachment-opaque-ref\"]");
        verify(mapper).insertDetail(eq(83L), eq("토론형 수업 개선"), eq(2025), eq(2));
        verify(mapper).insertStatusHistory(
                eq(83L),
                eq(null),
                any(),
                eq("CREATE"),
                eq("강의개선 실적 최초 입력"),
                eq(101L),
                any());
        verify(mapper).insertChangeHistory(
                eq("83"),
                eq("CREATE"),
                eq(null),
                eq("토론형 수업 개선|2025|2"),
                eq(101L),
                eq("강의개선 실적 저장"),
                eq("REQ-B83-LI-CREATE"));
    }

    @Test
    void updateRejectsConfirmedRowsBeforeAnyMutation() {
        LectureImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureImprovementAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        LectureImprovementAchievementService service = new LectureImprovementAchievementService(
                mapper,
                guardService,
                new ObjectMapper());
        CurrentUser requester = new CurrentUser(
                101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
        Request request = new Request(
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-11-01"),
                "수정 시도",
                2025,
                2,
                List.of());
        when(mapper.findById(83L)).thenReturn(row("EVALUATION_CONFIRMED", "기존 내용"));

        assertThatThrownBy(() -> service.update(83L, request, requester, "REQ-B83-LI-CONFLICT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).updateAchievement(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any(), any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateWritesAChangeHistoryAfterOwnershipAndGuardValidation() {
        LectureImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureImprovementAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        LectureImprovementAchievementService service = new LectureImprovementAchievementService(
                mapper,
                guardService,
                new ObjectMapper());
        CurrentUser requester = new CurrentUser(
                101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
        Request request = new Request(
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-10-01"),
                "개선 후 내용",
                2025,
                2,
                List.of());
        Row existing = row("DRAFT", "기존 내용");
        Row saved = row("DRAFT", "개선 후 내용");
        when(mapper.findById(83L)).thenReturn(existing, saved);
        when(guardService.validateMutation(eq(requester), any()))
                .thenReturn(OccurredDateValidation.accepted());

        service.update(83L, request, requester, "REQ-B83-LI-UPDATE");

        verify(mapper).updateAchievement(
                eq(83L),
                eq("LECTURE_IMPROVEMENT"),
                eq(LocalDate.parse("2025-10-01")),
                eq("2025학년도 2학기 강의개선"),
                eq("[]"),
                eq(101L));
        verify(mapper).updateDetail(eq(83L), eq("개선 후 내용"), eq(2025), eq(2));
        verify(mapper).insertChangeHistory(
                eq("83"),
                eq("UPDATE"),
                eq("기존 내용|2025|2"),
                eq("개선 후 내용|2025|2"),
                eq(101L),
                eq("강의개선 실적 수정"),
                eq("REQ-B83-LI-UPDATE"));
    }

    @Test
    void updateRejectsAnotherTeachersRowBeforeCheckingItsLockState() {
        LectureImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureImprovementAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        LectureImprovementAchievementService service = new LectureImprovementAchievementService(
                mapper,
                guardService,
                new ObjectMapper());
        CurrentUser requester = new CurrentUser(
                101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
        Request request = new Request(
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-11-01"),
                "수정 시도",
                2025,
                2,
                List.of());
        when(mapper.findById(83L)).thenReturn(row(202L, "EVALUATION_CONFIRMED", "타인 확정 내용"));

        assertThatThrownBy(() -> service.update(83L, request, requester, "REQ-B83-LI-CROSS-OWNER"))
                .isInstanceOf(ForbiddenException.class);

        verify(mapper, never()).updateAchievement(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any(), any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any());
    }

    private Row row(String achievementStatus, String achievementContent) {
        return row(101L, achievementStatus, achievementContent);
    }

    private Row row(Long teacherUserId, String achievementStatus, String achievementContent) {
        return new Row(
                83L,
                teacherUserId,
                "faculty",
                "KNUE-DEPT-COMP",
                "2025",
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-09-01"),
                "2025학년도 2학기 강의개선",
                achievementStatus,
                "[]",
                achievementContent,
                2025,
                2,
                LocalDateTime.parse("2025-09-01T09:00:00"),
                LocalDateTime.parse("2025-09-01T09:00:00"));
    }
}
