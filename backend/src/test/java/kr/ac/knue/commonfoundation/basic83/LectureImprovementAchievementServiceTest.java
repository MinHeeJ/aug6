package kr.ac.knue.commonfoundation.basic83;

import static org.assertj.core.api.Assertions.assertThatCode;
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
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

/** Verifies teaching-improvement writes preserve finalized rows and record audit side effects. */
class LectureImprovementAchievementServiceTest {
    @Test
    void createWritesSourceStatusAndChangeHistory() {
        LectureImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureImprovementAchievementMapper.class
        );
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class
        );
        LectureImprovementAchievementService service = new LectureImprovementAchievementService(mapper, guard);
        when(guard.validateMutation(eq(user()), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.findByManagementNo(any())).thenReturn(row());

        service.create(request(), user(), "REQ-LIA-CREATE");

        verify(mapper).insert(
                any(),
                eq(101L),
                eq("2026"),
                eq("LECTURE"),
                eq(LocalDate.parse("2026-04-10")),
                eq("수업 개선안"),
                eq(2025),
                eq(1),
                eq("file-1"),
                eq(101L)
        );
        verify(mapper).insertStatusHistory(any());
        verify(mapper).insertChangeHistory(
                eq("teaching_improvement_achievements"),
                eq("83"),
                eq("CREATE"),
                eq("achievement"),
                eq(null),
                eq("B83-LIA-001"),
                eq(101L),
                eq("강의개선 실적 저장"),
                eq("REQ-LIA-CREATE")
        );
    }

    @Test
    void confirmedUpdateFailsBeforeAnyMutation() {
        LectureImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureImprovementAchievementMapper.class
        );
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class
        );
        LectureImprovementAchievementService service = new LectureImprovementAchievementService(mapper, guard);
        when(mapper.findById(83L)).thenReturn(row());
        when(guard.validateMutation(eq(user()), any())).thenThrow(
                new ConflictException("CONFIRMED_DATA_LOCKED")
        );

        assertThatThrownBy(() -> service.update(83L, request(), user(), "REQ-LIA-UPDATE"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).update(any(), any(), any(), any(), any(), any(), any(), any());
        verify(mapper, never()).insertChangeHistory(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void systemAdministratorCanReadAllLectureImprovementRows() {
        LectureImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureImprovementAchievementMapper.class);
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        LectureImprovementAchievementService service = new LectureImprovementAchievementService(mapper, guard);
        CurrentUser administrator = new CurrentUser(
                1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

        assertThatCode(() -> service.list(new LectureImprovementSearchCriteria(0, 20), administrator))
                .doesNotThrowAnyException();
    }

    private CurrentUser user() {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    }

    private LectureImprovementRequest request() {
        return new LectureImprovementRequest(
                "LECTURE",
                LocalDate.parse("2026-04-10"),
                "수업 개선안",
                2025,
                1,
                List.of("file-1")
        );
    }

    private LectureImprovementAchievementRow row() {
        return new LectureImprovementAchievementRow(
                83L,
                "B83-LIA-001",
                101L,
                "faculty",
                "2026",
                "LECTURE",
                LocalDate.parse("2026-04-10"),
                "수업 개선안",
                2025,
                1,
                "file-1",
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00")
        );
    }
}
