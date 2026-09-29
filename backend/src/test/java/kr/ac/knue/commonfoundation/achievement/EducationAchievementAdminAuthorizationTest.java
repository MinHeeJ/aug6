package kr.ac.knue.commonfoundation.achievement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.junit.jupiter.api.Test;

/** Regression coverage: the seeded R09 administrator must read every education-achievement list. */
class EducationAchievementAdminAuthorizationTest {
    private final CurrentUser administrator = new CurrentUser(1L, "administrator", "E0001", "관리자", List.of("R09"), List.of());

    @Test
    void administratorCanListAllEducationAchievementTypes() {
        LectureAchievementMapper lectureMapper = mock(LectureAchievementMapper.class);
        when(lectureMapper.list(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(lectureMapper.count(org.mockito.ArgumentMatchers.any())).thenReturn(0L);

        LectureEvaluationAchievementMapper evaluationMapper = mock(LectureEvaluationAchievementMapper.class);
        when(evaluationMapper.list(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(evaluationMapper.count(org.mockito.ArgumentMatchers.any())).thenReturn(0L);

        DegreeCompletionAchievementMapper degreeMapper = mock(DegreeCompletionAchievementMapper.class);
        when(degreeMapper.list(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(degreeMapper.count(org.mockito.ArgumentMatchers.any())).thenReturn(0L);

        assertDoesNotThrow(() -> new LectureAchievementService(lectureMapper, new ObjectMapper())
                .list(administrator, null, null, null, null, null, null, null, 0, 20));
        assertDoesNotThrow(() -> new LectureEvaluationAchievementService(evaluationMapper, new ObjectMapper())
                .list(administrator, null, null, null, null, null, null, null, 0, 20));
        assertDoesNotThrow(() -> new DegreeCompletionAchievementService(degreeMapper, new ObjectMapper())
                .list(administrator, null, null, null, null, null, 0, 20));
    }
}
