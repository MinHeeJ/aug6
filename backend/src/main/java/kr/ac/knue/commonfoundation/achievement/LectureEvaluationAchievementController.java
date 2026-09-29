package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the authorized lecture-evaluation list and save operations defined by the education API contract. */
@RestController
public class LectureEvaluationAchievementController {
    private final LectureEvaluationAchievementService service;

    public LectureEvaluationAchievementController(LectureEvaluationAchievementService service) {
        this.service = service;
    }

    @GetMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementSearchResponse> listLectureEvaluationAchievements(
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) LocalDate occurredDateFrom,
            @RequestParam(required = false) LocalDate occurredDateTo,
            @RequestParam(required = false) String certificationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(currentUser(request), evaluationYear, managementNo, teacherName,
                managementItemCode, occurredDateFrom, occurredDateTo, certificationStatus, page, pageSize),
                request.getHeader("X-Request-Id"));
    }

    @PostMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementSaveResponse> saveLectureEvaluationAchievement(
            @RequestBody SaveLectureEvaluationAchievementRequest saveRequest, HttpServletRequest request) {
        return ApiResponse.ok(service.save(currentUser(request), saveRequest, request.getHeader("X-Request-Id")),
                request.getHeader("X-Request-Id"));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
