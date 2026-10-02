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

/** Verifies guarded BASIC-83 writes retain lifecycle and audit side effects. */
class EmploymentRateImprovementAchievementServiceTest {
    @Test
    void createWritesSourceStatusAndChangeHistory() {
        EmploymentRateImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                EmploymentRateImprovementAchievementMapper.class);
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        EmploymentRateImprovementAchievementService service = new EmploymentRateImprovementAchievementService(
                mapper,
                guard);
        CurrentUser requester = user();
        when(guard.validateMutation(eq(requester), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.findByManagementNo(any())).thenReturn(row());

        service.create(request(), requester, "REQ-B83-ERI-CREATE");

        verify(mapper).insert(
                any(),
                eq(101L),
                eq("2026"),
                eq("EMPLOYMENT_RATE"),
                eq(LocalDate.parse("2026-04-10")),
                eq(LocalDate.parse("2026-04-01")),
                eq(LocalDate.parse("2026-04-03")),
                eq("2026-04-15~2026-04-16"),
                eq("file-1"),
                eq(101L));
        verify(mapper).insertStatusHistory(any());
        verify(mapper).insertChangeHistory(
                eq("employment_rate_improvement_achievements"),
                eq("81"),
                eq("CREATE"),
                eq("achievement"),
                eq(null),
                eq("B83-ERI-001"),
                eq(101L),
                eq("취업률 제고 실적 저장"),
                eq("REQ-B83-ERI-CREATE"));
    }

    @Test
    void confirmedUpdateFailsBeforeMutation() {
        EmploymentRateImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                EmploymentRateImprovementAchievementMapper.class);
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        EmploymentRateImprovementAchievementService service = new EmploymentRateImprovementAchievementService(
                mapper,
                guard);
        when(mapper.findById(81L)).thenReturn(row());
        when(guard.validateMutation(eq(user()), any())).thenThrow(
                new ConflictException("CONFIRMED_DATA_LOCKED"));

        assertThatThrownBy(() -> service.update(81L, request(), user(), "REQ-B83-ERI-UPDATE"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).update(any(), any(), any(), any(), any(), any(), any(), any());
        verify(mapper, never()).insertChangeHistory(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void systemAdministratorCanReadAllEmploymentRateImprovementRows() {
        EmploymentRateImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                EmploymentRateImprovementAchievementMapper.class);
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        EmploymentRateImprovementAchievementService service = new EmploymentRateImprovementAchievementService(
                mapper, guard);
        CurrentUser administrator = new CurrentUser(
                1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

        assertThatCode(() -> service.list(
                new EmploymentRateImprovementSearchCriteria(0, 20, null, null, null, null),
                administrator)).doesNotThrowAnyException();
    }

    private CurrentUser user() {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    }

    private EmploymentRateImprovementRequest request() {
        return new EmploymentRateImprovementRequest(
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-03"),
                "2026-04-15~2026-04-16",
                List.of("file-1"));
    }

    private EmploymentRateImprovementRow row() {
        return new EmploymentRateImprovementRow(
                81L,
                "B83-ERI-001",
                101L,
                "faculty",
                "2026",
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-03"),
                "2026-04-15~2026-04-16",
                "file-1",
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}
