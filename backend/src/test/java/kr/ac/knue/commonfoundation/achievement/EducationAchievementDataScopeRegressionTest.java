package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Regression coverage for REQ-1904: a role may not mutate a lecture-evaluation row outside its data scope.
 */
class EducationAchievementDataScopeRegressionTest {
    @Test
    void departmentRoleOutsideRowScopeIsRejectedBeforePeriodOrPersistenceChecks() {
        EducationAchievementFoundationMapper mapper = Mockito.mock(EducationAchievementFoundationMapper.class);
        EducationAchievementFoundationService service = new EducationAchievementFoundationService(mapper);
        CurrentUser departmentChair = new CurrentUser(20L, "chair", "E0020", "학과장", List.of("R02"), List.of());
        EducationAchievementMutationContext context = new EducationAchievementMutationContext(77L, "2026", "ORG-OUTSIDE",
                "B77-LE-001", LocalDate.parse("2026-03-15"), departmentChair, "UPDATE");
        when(mapper.countAllowedFunctionPermission(EducationAchievementFoundationService.SCREEN_ID, "R02", "UPDATE")).thenReturn(1);
        when(mapper.countLectureEvaluationScope(77L, 20L)).thenReturn(0);

        assertThatThrownBy(() -> service.validateMutation(context)).isInstanceOf(ForbiddenException.class);

        verify(mapper, never()).countActiveInputPeriod("2026", "ORG-OUTSIDE");
        verify(mapper, never()).findLectureEvaluationForUpdate(77L);
        verify(mapper, never()).updateLectureEvaluationStatus(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }
}
