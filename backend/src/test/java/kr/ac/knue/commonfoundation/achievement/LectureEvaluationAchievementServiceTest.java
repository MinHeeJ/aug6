package kr.ac.knue.commonfoundation.achievement;

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

class LectureEvaluationAchievementServiceTest {
    @Test
    void savePersistsCreateChangeHistoryAndReturnsReloadedAchievementForOutOfPeriodWarning() {
        LectureEvaluationAchievementMapper mapper = org.mockito.Mockito.mock(LectureEvaluationAchievementMapper.class);
        EducationAchievementAccessValidator validator = org.mockito.Mockito.mock(EducationAchievementAccessValidator.class);
        EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();
        LectureEvaluationAchievementService service = new LectureEvaluationAchievementService(
                mapper,
                validator,
                policy,
                new ObjectMapper()
        );
        CurrentUser teacher = new CurrentUser(101L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
        LectureEvaluationAchievementRow stored = row(790010L, "LE-2026-created", "DRAFT");
        when(mapper.findActiveOrganizationCode(101L)).thenReturn("KNUE-DEPT-COMP");
        when(validator.validateMutation(eq(teacher), any(EducationAchievementMutationContext.class)))
                .thenReturn(new EducationAchievementValidationResult(true));
        when(mapper.findByManagementNo(any())).thenReturn(stored);

        LectureEvaluationAchievementRow saved = service.save(
                new SaveLectureEvaluationAchievementRequest(
                        null,
                        null,
                        null,
                        null,
                        "EDU-LECTURE-EVALUATION",
                        LocalDate.parse("2026-02-28"),
                        new ObjectMapper().createObjectNode().put("lectureName", "교육과정"),
                        "opaque-reference",
                        "강의평가 등록"
                ),
                teacher
        );

        assertThat(saved.managementNo()).isEqualTo("LE-2026-created");
        verify(mapper).insertAchievement(
                any(),
                eq(101L),
                eq("2026"),
                eq("KNUE-DEPT-COMP"),
                eq("EDU-LECTURE-EVALUATION"),
                eq(LocalDate.parse("2026-02-28")),
                any(),
                eq("opaque-reference"),
                eq(101L),
                eq("강의평가 등록")
        );
        verify(mapper).insertChangeHistory(
                eq(790010L),
                eq("CREATE"),
                eq("achievement"),
                eq(null),
                any(),
                eq(101L),
                eq("강의평가 등록")
        );
    }

    private LectureEvaluationAchievementRow row(Long id, String managementNo, String status) {
        return new LectureEvaluationAchievementRow(
                id,
                managementNo,
                101L,
                "홍길동",
                "2026",
                "KNUE-DEPT-COMP",
                "EDU-LECTURE-EVALUATION",
                LocalDate.parse("2026-02-28"),
                "{\"lectureName\":\"교육과정\"}",
                status,
                true,
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00")
        );
    }
}
