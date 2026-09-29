package kr.ac.knue.commonfoundation.health;

import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.faculty.achievement.LectureAchievementController;
import kr.ac.knue.commonfoundation.faculty.achievement.LectureEvaluationAchievementController;
import kr.ac.knue.commonfoundation.faculty.achievement.StudentGuidanceAchievementController;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Provides the service health endpoint and imports focused education-achievement MVC contract controllers. */
@RestController
@Import({LectureEvaluationAchievementController.class, LectureAchievementController.class, StudentGuidanceAchievementController.class})
public class HealthController {
    @GetMapping("/api/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP", "service", "common-foundation"));
    }
}
