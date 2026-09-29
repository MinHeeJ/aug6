package kr.ac.knue.commonfoundation.achievement;

import kr.ac.knue.commonfoundation.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Starts the MVC mapping registry with the health and achievement controllers that
 * own the BASIC-78 routes. Duplicate HTTP method/path mappings must prevent this
 * context from starting.
 */
@WebMvcTest({
        HealthController.class,
        LectureAchievementController.class,
        LectureEvaluationAchievementController.class,
        DegreeCompletionAchievementController.class
})
class AchievementRouteMappingContextTest {

    @MockBean
    private LectureAchievementService lectureAchievementService;

    @MockBean
    private LectureEvaluationAchievementService lectureEvaluationAchievementService;

    @MockBean
    private DegreeCompletionAchievementService degreeCompletionAchievementService;

    @Test
    void registersEachAchievementGetRouteOnce() {
        // A duplicate route fails WebMvcTest context initialization before this executes.
    }
}
