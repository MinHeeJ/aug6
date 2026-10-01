package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

/** HTTP boundary for authorized lecture-achievement search, save, and certification actions. */
@RestController
public class LectureAchievementController {
    private final LectureAchievementService service;

    public LectureAchievementController(LectureAchievementService service) { this.service = service; }

    /** Lists lecture achievements using the OpenAPI bounded page sizes and optional filters. */
    @GetMapping("/api/business/lecture-achievements")
    public ApiResponse<LectureAchievementSearchResponse> listLectureAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String certificationStatus,
            HttpServletRequest request
    ) {
        CurrentUser actor = actor(request);
        return ApiResponse.ok(service.list(new LectureAchievementSearchCriteria(
                page, size, managementNo, teacherName, managementItemCode, certificationStatus, null, null
        ), actor));
    }

    /** Saves a lecture achievement after the service validates scope, period, and finalization lock. */
    @PostMapping("/api/business/lecture-achievements")
    public ApiResponse<LectureAchievementRow> saveLectureAchievement(
            @Valid @RequestBody SaveLectureAchievementRequest request,
            HttpServletRequest servletRequest
    ) { return ApiResponse.ok(service.save(request, actor(servletRequest))); }

    /** Transitions a non-finalized lecture achievement and preserves its status history. */
    @PostMapping("/api/business/lecture-achievements/{achievementId}/transitions")
    public ApiResponse<LectureAchievementRow> transitionLectureAchievement(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureAchievementTransitionRequest request,
            HttpServletRequest servletRequest
    ) {
        String requestId = servletRequest.getHeader("X-Request-Id");
        return ApiResponse.ok(service.transition(achievementId, request, actor(servletRequest), requestId), requestId);
    }

    private CurrentUser actor(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (!(user instanceof CurrentUser currentUser)) throw new UnauthenticatedException();
        if (currentUser.roles().stream().noneMatch(
                role -> role.equals("R01") || role.equals("R02") || role.equals("R04") || role.equals("R09")
        )) throw new ForbiddenException();
        return currentUser;
    }
}
