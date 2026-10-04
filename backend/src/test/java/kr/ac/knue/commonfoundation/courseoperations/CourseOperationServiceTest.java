package kr.ac.knue.commonfoundation.courseoperations;

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
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

/** Verifies course-operation updates validate and persist the request's evaluation year atomically. */
class CourseOperationServiceTest {
    @Test
    void updateMovesAchievementToTheRequestedEvaluationYear() {
        CourseOperationMapper mapper = org.mockito.Mockito.mock(CourseOperationMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        CourseOperationService service = new CourseOperationService(mapper, guardService);
        CurrentUser requester = r01();
        LocalDate targetDate = LocalDate.parse("2027-01-15");
        CourseOperationRow existing = row("2026", LocalDate.parse("2026-11-20"), "기존 실적내역");
        CourseOperationRow updated = row("2027", targetDate, "수정한 실적내역");
        CourseOperationRequest request = new CourseOperationRequest(
                "COURSE_OPERATION_NEW",
                targetDate,
                "수정한 실적내역",
                List.of("attachment-2027"));
        when(mapper.findScoped(83L, requester.userId(), requester.roles())).thenReturn(existing, updated);

        CourseOperationRow result = service.update(83L, request, requester, "REQ-B83-CO-CROSS-YEAR");

        assertThat(result.evaluationYear()).isEqualTo("2027");
        verify(guardService).validateMutation(
                eq(requester),
                eq(new EducationAchievementMutationContext(101L, "2027", targetDate)));
        verify(mapper).updateHeader(
                eq(83L),
                eq("2027"),
                eq("COURSE_OPERATION_NEW"),
                eq(targetDate),
                eq("attachment-2027"),
                eq(101L));
    }

    @Test
    void updateRejectsWhenTheTargetEvaluationYearIsFinalized() {
        CourseOperationMapper mapper = org.mockito.Mockito.mock(CourseOperationMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        CourseOperationService service = new CourseOperationService(mapper, guardService);
        CurrentUser requester = r01();
        LocalDate targetDate = LocalDate.parse("2027-01-15");
        CourseOperationRequest request = new CourseOperationRequest(
                "COURSE_OPERATION",
                targetDate,
                "확정 연도 수정 시도",
                null);
        when(mapper.findScoped(83L, requester.userId(), requester.roles()))
                .thenReturn(row("2026", LocalDate.parse("2026-11-20"), "기존 실적내역"));
        when(guardService.validateMutation(
                eq(requester),
                eq(new EducationAchievementMutationContext(101L, "2027", targetDate))))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED"));

        assertThatThrownBy(() -> service.update(83L, request, requester, "REQ-B83-CO-TARGET-CONFIRMED"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).updateHeader(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any(), any());
        verify(mapper, never()).insertChangeHistory(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    private CurrentUser r01() {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    }

    private CourseOperationRow row(String evaluationYear, LocalDate achievementDate, String performanceDetails) {
        return new CourseOperationRow(
                83L,
                "B83-CO-001",
                101L,
                "faculty",
                evaluationYear,
                "COURSE_OPERATION",
                achievementDate,
                performanceDetails,
                performanceDetails,
                "DRAFT",
                null,
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}
