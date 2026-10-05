package kr.ac.knue.commonfoundation.courseoperations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies immutable lifecycle and evaluation-year guards for course-operation updates. */
@ExtendWith(MockitoExtension.class)
class CourseOperationServiceTest {
    @Mock
    private CourseOperationMapper mapper;

    @Mock
    private EducationAchievementGuardService guardService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CourseOperationService service;

    @Test
    void updateRejectsFinalizedAchievementBeforeGuardOrPersistenceMutation() {
        when(mapper.findById(71L)).thenReturn(row("EVALUATION_CONFIRMED", "2026"));

        assertThatThrownBy(() -> service.update(71L, request("2026-04-12"), r01User()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ACHIEVEMENT_NOT_EDITABLE");

        verifyNoInteractions(guardService);
        verify(mapper, never()).updateHeader(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateRejectsAchievementDateOutsideExistingEvaluationYearWithoutMutation() {
        when(mapper.findById(71L)).thenReturn(row("DRAFT", "2026"));

        assertThatThrownBy(() -> service.update(71L, request("2027-01-01"), r01User()))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("기존 평가연도");

        verifyNoInteractions(guardService);
        verify(mapper, never()).updateHeader(
                eq(71L),
                any(),
                any(),
                any(),
                any(),
                any());
        verify(mapper, never()).updateDetail(any(), any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any());
    }

    private CurrentUser r01User() {
        return new CurrentUser(101L, "professor1", "E0101", "교원", List.of("R01"), List.of());
    }

    private CourseOperationRequest request(String achievementDate) {
        return new CourseOperationRequest(
                "COURSE_OPERATION",
                LocalDate.parse(achievementDate),
                "현장 연계 강좌 운영",
                List.of());
    }

    private CourseOperationRow row(String status, String evaluationYear) {
        return new CourseOperationRow(
                71L,
                "B83-CO-001",
                101L,
                "professor1",
                evaluationYear,
                "COURSE_OPERATION",
                LocalDate.parse("2026-04-11"),
                "현장 연계 강좌 운영",
                "현장 연계 강좌 운영",
                status,
                "[]",
                LocalDateTime.parse("2026-04-11T09:00:00"),
                LocalDateTime.parse("2026-04-11T09:00:00"));
    }
}
