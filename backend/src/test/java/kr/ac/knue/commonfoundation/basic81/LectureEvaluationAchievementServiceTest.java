package kr.ac.knue.commonfoundation.basic81;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the lecture-evaluation save transaction's initial status and
 * data-change audit effects independently from the HTTP adapter.
 */
class LectureEvaluationAchievementServiceTest {
    @Test
    void savePersistsAWarningEligibleRowWithStatusAndDataChangeHistory() {
        LectureEvaluationAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureEvaluationAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        LectureEvaluationAchievementService service = new LectureEvaluationAchievementService(
                mapper,
                guardService,
                new EducationAchievementStatusTransitionPolicy(),
                new ObjectMapper());
        CurrentUser requester = new CurrentUser(
                101L,
                "faculty",
                "E0101",
                "교원",
                List.of("R01"),
                List.of());
        SaveLectureEvaluationAchievementRequest request =
                new SaveLectureEvaluationAchievementRequest(
                        "LECTURE_EVALUATION",
                        LocalDate.parse("2025-12-31"),
                        new ObjectMapper().createObjectNode().put("score", 95),
                        "attachment-opaque-ref");
        LectureEvaluationAchievementRow saved = new LectureEvaluationAchievementRow(
                81L,
                "LE-save-test",
                101L,
                "faculty",
                "2025",
                "LECTURE_EVALUATION",
                LocalDate.parse("2025-12-31"),
                "{\"score\":95}",
                "DRAFT",
                "attachment-opaque-ref",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
        when(guardService.validateMutation(eq(requester), any()))
                .thenReturn(OccurredDateValidation.outsideEvaluationPeriod());
        when(mapper.findLectureEvaluationAchievement(any())).thenReturn(saved);

        LectureEvaluationAchievementSaveResult result = service.save(
                request,
                requester,
                "REQ-B81-SERVICE-SAVE");

        assertThat(result.achievement().managementItemCode())
                .isEqualTo("LECTURE_EVALUATION");
        assertThat(result.occurredDateWarning()).isTrue();
        verify(mapper).insertLectureEvaluationAchievement(
                any(),
                eq(101L),
                eq("2025"),
                eq("LECTURE_EVALUATION"),
                eq(LocalDate.parse("2025-12-31")),
                eq("{\"score\":95}"),
                eq("attachment-opaque-ref"),
                eq(101L));
        verify(mapper).insertStatusHistory(any(EducationAchievementStatusHistory.class));
        verify(mapper).insertChangeHistory(
                eq("lecture_evaluation_achievements"),
                eq("81"),
                eq("CREATE"),
                eq("achievement_detail"),
                eq(null),
                eq("{\"score\":95}"),
                eq(101L),
                eq("강의평가 실적 저장"),
                eq("REQ-B81-SERVICE-SAVE"));
    }
}
