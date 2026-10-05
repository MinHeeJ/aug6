package kr.ac.knue.commonfoundation.courseoperations;

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

/** Verifies atomic persistence orchestration and modification locks for course operations. */
class CourseOperationServiceTest {
    @Test
    void createPersistsSourceDetailStatusAndAuditHistory() {
        CourseOperationMapper mapper = org.mockito.Mockito.mock(CourseOperationMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        CourseOperationService service = new CourseOperationService(
                mapper,
                guardService,
                new ObjectMapper());
        CurrentUser requester = requester();
        CourseOperationRequest request = request("신규 강좌 운영", LocalDate.parse("2026-04-10"));
        CourseOperationRow saved = row("신규 강좌 운영", LocalDate.parse("2026-04-10"));
        when(mapper.findActiveOrganizationCode(101L)).thenReturn("ENG");
        when(guardService.validateMutation(eq(requester), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.insertAchievement(any())).thenReturn(82L);
        when(mapper.findCourseOperation(82L)).thenReturn(saved);

        CourseOperationSaveResponse result = service.create(request, requester, "REQ-B83-CO-CREATE");

        assertThat(result.achievement().performanceDetails()).isEqualTo("신규 강좌 운영");
        verify(mapper).insertAchievement(any());
        verify(mapper).insertDetails(82L, "신규 강좌 운영", 101L);
        verify(mapper).insertStatusHistory(any());
        verify(mapper).insertChangeHistory(
                eq("education_achievements"),
                eq("82"),
                eq("CREATE"),
                eq("course_operation"),
                eq(null),
                eq("신규 강좌 운영"),
                eq(101L),
                org.mockito.ArgumentMatchers.contains("REQ-B83-CO-CREATE"));
    }

    @Test
    void updateRejectsConfirmedDataBeforeAnyPersistenceMutation() {
        CourseOperationMapper mapper = org.mockito.Mockito.mock(CourseOperationMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        CourseOperationService service = new CourseOperationService(
                mapper,
                guardService,
                new ObjectMapper());
        CurrentUser requester = requester();
        when(mapper.findCourseOperation(82L)).thenReturn(row("기존 강좌 운영", LocalDate.parse("2026-04-10")));
        when(guardService.validateMutation(eq(requester), any()))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED"));

        assertThatThrownBy(() -> service.update(
                82L,
                request("수정 강좌 운영", LocalDate.parse("2026-04-11")),
                requester,
                "REQ-B83-CO-CONFIRMED"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).updateAchievement(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).updateDetails(any(), any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any(), any());
    }

    private CurrentUser requester() {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    }

    private CourseOperationRequest request(String details, LocalDate date) {
        return new CourseOperationRequest("COURSE", date, details, List.of("file-1"));
    }

    private CourseOperationRow row(String details, LocalDate date) {
        return new CourseOperationRow(
                82L,
                "CO-001",
                101L,
                "faculty",
                "ENG",
                "2026",
                "COURSE",
                date,
                details,
                "DRAFT",
                "[\"file-1\"]",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}
