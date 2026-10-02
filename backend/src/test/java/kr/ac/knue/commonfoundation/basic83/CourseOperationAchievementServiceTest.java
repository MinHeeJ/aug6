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

/** Verifies course-operation writes produce required history and preserve locked source rows. */
class CourseOperationAchievementServiceTest {
    @Test
    void createWritesSourceStatusAndChangeHistory() {
        CourseOperationAchievementMapper mapper = org.mockito.Mockito.mock(CourseOperationAchievementMapper.class);
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        CourseOperationAchievementService service = new CourseOperationAchievementService(mapper, guard);
        when(guard.validateMutation(eq(user()), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.findByManagementNo(any())).thenReturn(row());

        service.create(request(), user(), "REQ-COA-CREATE");

        verify(mapper).insert(any(), eq(101L), eq("2026"), eq("COURSE"),
                eq(LocalDate.parse("2026-04-10")), eq("운영 내역"), eq("file-1"), eq(101L));
        verify(mapper).insertStatusHistory(any());
        verify(mapper).insertChangeHistory(eq("course_offering_operation_achievements"), eq("82"),
                eq("CREATE"), eq("achievement"), eq(null), eq("B83-COA-001"), eq(101L),
                eq("강좌 개설·운영 실적 저장"), eq("REQ-COA-CREATE"));
    }

    @Test
    void confirmedUpdateFailsBeforeAnyMutation() {
        CourseOperationAchievementMapper mapper = org.mockito.Mockito.mock(CourseOperationAchievementMapper.class);
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        CourseOperationAchievementService service = new CourseOperationAchievementService(mapper, guard);
        when(mapper.findById(82L)).thenReturn(row());
        when(guard.validateMutation(eq(user()), any())).thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED"));

        assertThatThrownBy(() -> service.update(82L, request(), user(), "REQ-COA-UPDATE"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).update(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void systemAdministratorCanReadAllCourseOperationRows() {
        CourseOperationAchievementMapper mapper = org.mockito.Mockito.mock(CourseOperationAchievementMapper.class);
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        CourseOperationAchievementService service = new CourseOperationAchievementService(mapper, guard);
        CurrentUser administrator = new CurrentUser(
                1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

        assertThatCode(() -> service.list(new CourseOperationSearchCriteria(0, 20), administrator))
                .doesNotThrowAnyException();
    }

    private CurrentUser user() {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    }

    private CourseOperationRequest request() {
        return new CourseOperationRequest("COURSE", LocalDate.parse("2026-04-10"), "운영 내역", List.of("file-1"));
    }

    private CourseOperationAchievementRow row() {
        return new CourseOperationAchievementRow(82L, "B83-COA-001", 101L, "faculty", "2026", "COURSE",
                LocalDate.parse("2026-04-10"), "운영 내역", "file-1", "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}
