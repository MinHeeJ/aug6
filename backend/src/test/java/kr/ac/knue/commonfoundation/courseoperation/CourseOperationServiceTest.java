package kr.ac.knue.commonfoundation.courseoperation;

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
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

/** Verifies guarded course-operation write behavior and data-change history side effects. */
class CourseOperationServiceTest {
    @Test
    void createPersistsTheSourceAndDataChangeHistoryAfterGuardValidation() {
        CourseOperationMapper mapper = org.mockito.Mockito.mock(CourseOperationMapper.class);
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        CourseOperationService service = new CourseOperationService(mapper, guard, new ObjectMapper());
        CurrentUser requester = requester();
        CourseOperationRequest request = new CourseOperationRequest(
                "COURSE_OPERATION",
                LocalDate.parse("2026-04-10"),
                "신규 강좌 운영",
                List.of("file-83"));
        when(guard.validateMutation(eq(requester), any()))
                .thenReturn(OccurredDateValidation.accepted());
        when(mapper.findByManagementNo(any())).thenReturn(row("신규 강좌 운영"));

        CourseOperationResponse result = service.create(request, requester, "REQ-B83-CO-CREATE");

        assertThat(result.performanceDetails()).isEqualTo("신규 강좌 운영");
        assertThat(result.attachmentIds()).containsExactly("file-83");
        verify(mapper).insert(
                any(),
                eq(101L),
                eq("2026"),
                eq("COURSE_OPERATION"),
                eq(LocalDate.parse("2026-04-10")),
                eq("신규 강좌 운영"),
                eq("[\"file-83\"]"),
                eq(101L));
        verify(mapper).insertChangeHistory(
                eq("83"),
                eq("CREATE"),
                eq(null),
                eq("신규 강좌 운영"),
                eq(101L),
                eq("강좌 개설·운영 실적 저장"),
                eq("REQ-B83-CO-CREATE"));
    }

    @Test
    void updateRejectsConfirmedDataBeforeAnyMutation() {
        CourseOperationMapper mapper = org.mockito.Mockito.mock(CourseOperationMapper.class);
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        CourseOperationService service = new CourseOperationService(mapper, guard, new ObjectMapper());
        CurrentUser requester = requester();
        CourseOperationRequest request = new CourseOperationRequest(
                "COURSE_OPERATION",
                LocalDate.parse("2026-04-11"),
                "변경된 강좌 운영",
                List.of());
        when(mapper.findById(83L)).thenReturn(row("기존 강좌 운영"));
        when(mapper.countAccessible(83L, 101L, List.of("R01"))).thenReturn(1);
        when(guard.validateMutation(eq(requester), any()))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED"));

        assertThatThrownBy(() -> service.update(83L, request, requester, "REQ-B83-CO-CONFIRMED"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).update(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any());
    }

    private CurrentUser requester() {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    }

    private CourseOperationRow row(String performanceDetails) {
        return new CourseOperationRow(
                83L,
                "B83-CO-001",
                101L,
                "faculty",
                "2026",
                "COURSE_OPERATION",
                LocalDate.parse("2026-04-10"),
                performanceDetails,
                "[\"file-83\"]",
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}
