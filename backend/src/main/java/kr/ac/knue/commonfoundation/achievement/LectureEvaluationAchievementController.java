package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP boundary for authorized lecture-evaluation achievement search, save, and certification
 * transitions. The SessionCookie filter establishes the principal consumed here.
 */
@RestController
public class LectureEvaluationAchievementController {
    private final LectureEvaluationAchievementService service;

    public LectureEvaluationAchievementController(LectureEvaluationAchievementService service) {
        this.service = service;
    }

    /** Lists lecture-evaluation achievements with the contract's bounded page-size choices. */
    @GetMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementSearchResponse> listLectureEvaluationAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) LocalDate occurredDateFrom,
            @RequestParam(required = false) LocalDate occurredDateTo,
            @RequestParam(required = false) String certificationStatus,
            HttpServletRequest request
    ) {
        CurrentUser actor = requireAchievementRole(request);
        return ApiResponse.ok(service.list(new LectureEvaluationAchievementSearchCriteria(page, size, managementNo, teacherName, managementItemCode, occurredDateFrom, occurredDateTo, certificationStatus, null, null), actor));
    }

    /** Saves a new or selected existing achievement after server-side validation. */
    @PostMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementRow> saveLectureEvaluationAchievement(
            @Valid @RequestBody SaveLectureEvaluationAchievementRequest request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser actor = requireAchievementRole(servletRequest);
        return ApiResponse.ok(service.save(request, actor));
    }

    /** Records a permitted certification status action with the request identifier for tracing. */
    @PostMapping("/api/business/lecture-evaluation-achievements/{achievementId}/transitions")
    public ApiResponse<LectureEvaluationAchievementRow> transitionLectureEvaluationAchievement(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureEvaluationAchievementTransitionRequest request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser actor = requireAchievementRole(servletRequest);
        return ApiResponse.ok(service.transition(achievementId, request, actor, servletRequest.getHeader("X-Request-Id")), servletRequest.getHeader("X-Request-Id"));
    }

    private CurrentUser requireAchievementRole(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (!(user instanceof CurrentUser currentUser)) {
            throw new UnauthenticatedException();
        }
        if (currentUser.roles().stream().noneMatch(role -> role.equals("R01") || role.equals("R02") || role.equals("R04") || role.equals("R09"))) {
            throw new ForbiddenException();
        }
        return currentUser;
    }
}
