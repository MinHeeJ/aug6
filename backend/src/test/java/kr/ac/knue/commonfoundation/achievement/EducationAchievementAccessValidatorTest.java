package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.Test;

class EducationAchievementAccessValidatorTest {
    private static final LocalDateTime REQUESTED_AT = LocalDateTime.parse("2026-04-10T10:00:00");

    @Test
    void r01OwnAchievementWithinInputPeriodReturnsOccurrenceDateWarningWithoutBlockingSave() {
        EducationAchievementAccessMapper mapper = org.mockito.Mockito.mock(EducationAchievementAccessMapper.class);
        EducationAchievementAccessValidator validator = new EducationAchievementAccessValidator(mapper);
        CurrentUser r01 = user(101L, "R01");
        EducationAchievementMutationContext context = context(101L, LocalDate.parse("2026-02-28"));
        when(mapper.countActiveInputPeriods("2026", "EDUCATION", "KNUE-DEPT-COMP", REQUESTED_AT)).thenReturn(1);
        when(mapper.countConfirmedFinalizations(101L, "2026")).thenReturn(0);
        when(mapper.countEvaluationPeriodContainingDate(
                "2026",
                "EDUCATION",
                "KNUE-DEPT-COMP",
                LocalDate.parse("2026-02-28")
        )).thenReturn(0);

        EducationAchievementValidationResult result = validator.validateMutation(r01, context);

        assertThat(result.occurredDateWarning()).isTrue();
        verify(mapper, never()).countDepartmentScopedTarget(any(), any(), any());
    }

    @Test
    void r02OutsideDepartmentScopeIsForbiddenBeforeInputPeriodQuery() {
        EducationAchievementAccessMapper mapper = org.mockito.Mockito.mock(EducationAchievementAccessMapper.class);
        EducationAchievementAccessValidator validator = new EducationAchievementAccessValidator(mapper);
        CurrentUser r02 = user(201L, "R02");
        when(mapper.countDepartmentScopedTarget(201L, 101L, "KNUE-DEPT-COMP")).thenReturn(0);

        assertThatThrownBy(() -> validator.validateMutation(r02, context(101L, LocalDate.parse("2026-04-10"))))
                .isInstanceOf(ForbiddenException.class);

        verify(mapper, never()).countActiveInputPeriods(any(), any(), any(), any());
    }

    @Test
    void inactiveInputPeriodBlocksMutationBeforeFinalizationQuery() {
        EducationAchievementAccessMapper mapper = org.mockito.Mockito.mock(EducationAchievementAccessMapper.class);
        EducationAchievementAccessValidator validator = new EducationAchievementAccessValidator(mapper);
        when(mapper.countActiveInputPeriods("2026", "EDUCATION", "KNUE-DEPT-COMP", REQUESTED_AT)).thenReturn(0);

        assertThatThrownBy(() -> validator.validateMutation(user(101L, "R01"), context(101L, LocalDate.parse("2026-04-10"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("PERIOD_NOT_ACTIVE");

        verify(mapper, never()).countConfirmedFinalizations(any(), any());
    }

    @Test
    void confirmedFinalizationBlocksMutationBeforeOccurrenceDateWarningQuery() {
        EducationAchievementAccessMapper mapper = org.mockito.Mockito.mock(EducationAchievementAccessMapper.class);
        EducationAchievementAccessValidator validator = new EducationAchievementAccessValidator(mapper);
        when(mapper.countActiveInputPeriods("2026", "EDUCATION", "KNUE-DEPT-COMP", REQUESTED_AT)).thenReturn(1);
        when(mapper.countConfirmedFinalizations(101L, "2026")).thenReturn(1);

        assertThatThrownBy(() -> validator.validateMutation(user(101L, "R01"), context(101L, LocalDate.parse("2026-04-10"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).countEvaluationPeriodContainingDate(any(), any(), any(), any());
    }

    private CurrentUser user(Long userId, String role) {
        return new CurrentUser(userId, "user-" + userId, "E" + userId, "테스트 사용자", List.of(role), List.of());
    }

    private EducationAchievementMutationContext context(Long targetUserId, LocalDate occurredDate) {
        return new EducationAchievementMutationContext(
                targetUserId,
                "2026",
                "knue-dept-comp",
                occurredDate,
                REQUESTED_AT
        );
    }
}
