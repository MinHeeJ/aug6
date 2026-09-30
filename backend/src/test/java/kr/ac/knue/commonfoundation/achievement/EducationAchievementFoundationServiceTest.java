package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

class EducationAchievementFoundationServiceTest {
    @Test
    void departmentRejectionRecordsRequiredReasonAndImmutableStatusHistory() {
        EducationAchievementFoundationMapper mapper = org.mockito.Mockito.mock(EducationAchievementFoundationMapper.class);
        EducationAchievementFoundationService service = new EducationAchievementFoundationService(mapper);
        CurrentUser departmentChair = new CurrentUser(2L, "chair", "E0002", "학과장", List.of("R02"), List.of());
        LectureEvaluationAchievementState submitted = state("SUBMITTED");
        when(mapper.findLectureEvaluationForUpdate(101L)).thenReturn(submitted);
        when(mapper.countAllowedFunctionPermission(EducationAchievementFoundationService.SCREEN_ID, "R02", "UPDATE")).thenReturn(1);
        when(mapper.countLectureEvaluationScope(101L, 2L)).thenReturn(1);
        when(mapper.countEvaluationConfirmedFinalization(10L, "2026")).thenReturn(0);
        when(mapper.updateLectureEvaluationStatus(101L, "SUBMITTED", "DEPARTMENT_REJECTED", 2L)).thenReturn(1);

        service.transitionLectureEvaluation(101L,
                new EducationAchievementTransitionRequest("DEPARTMENT_REJECTED", "DEPT_REASON", null), departmentChair);

        verify(mapper).updateLectureEvaluationStatus(101L, "SUBMITTED", "DEPARTMENT_REJECTED", 2L);
        verify(mapper).insertStatusHistory("LECTURE_EVALUATION", 101L, "SUBMITTED", "DEPARTMENT_REJECTED",
                "REJECT", "DEPT_REASON", null, 2L);
    }

    @Test
    void rejectionWithoutReasonOrOpinionFailsBeforeStatusOrHistorySideEffects() {
        EducationAchievementFoundationMapper mapper = org.mockito.Mockito.mock(EducationAchievementFoundationMapper.class);
        EducationAchievementFoundationService service = new EducationAchievementFoundationService(mapper);
        CurrentUser departmentChair = new CurrentUser(2L, "chair", "E0002", "학과장", List.of("R02"), List.of());
        when(mapper.findLectureEvaluationForUpdate(101L)).thenReturn(state("SUBMITTED"));
        when(mapper.countAllowedFunctionPermission(EducationAchievementFoundationService.SCREEN_ID, "R02", "UPDATE")).thenReturn(1);
        when(mapper.countLectureEvaluationScope(101L, 2L)).thenReturn(1);
        when(mapper.countEvaluationConfirmedFinalization(10L, "2026")).thenReturn(0);

        assertThatThrownBy(() -> service.transitionLectureEvaluation(101L,
                new EducationAchievementTransitionRequest("DEPARTMENT_REJECTED", null, " "), departmentChair))
                .isInstanceOf(BusinessValidationException.class);

        verify(mapper, never()).updateLectureEvaluationStatus(any(), any(), any(), any());
        verify(mapper, never()).insertStatusHistory(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void disallowedTransitionKeepsConfirmedStatusAndDoesNotWriteHistory() {
        EducationAchievementFoundationMapper mapper = org.mockito.Mockito.mock(EducationAchievementFoundationMapper.class);
        EducationAchievementFoundationService service = new EducationAchievementFoundationService(mapper);
        CurrentUser teacher = new CurrentUser(10L, "teacher", "E0010", "교원", List.of("R01"), List.of());
        when(mapper.findLectureEvaluationForUpdate(101L)).thenReturn(state("DEPARTMENT_CONFIRMED"));
        when(mapper.countAllowedFunctionPermission(EducationAchievementFoundationService.SCREEN_ID, "R01", "UPDATE")).thenReturn(1);
        when(mapper.countLectureEvaluationScope(101L, 10L)).thenReturn(1);
        when(mapper.countEvaluationConfirmedFinalization(10L, "2026")).thenReturn(0);

        assertThatThrownBy(() -> service.transitionLectureEvaluation(101L,
                new EducationAchievementTransitionRequest("CERTIFIED", null, null), teacher))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INVALID_STATE_TRANSITION");

        verify(mapper, never()).updateLectureEvaluationStatus(eq(101L), any(), any(), any());
        verify(mapper, never()).insertStatusHistory(any(), any(), any(), any(), any(), any(), any(), any());
    }

    private LectureEvaluationAchievementState state(String certificationStatus) {
        return new LectureEvaluationAchievementState(101L, 10L, "2026", "EDU-001", certificationStatus);
    }
}
