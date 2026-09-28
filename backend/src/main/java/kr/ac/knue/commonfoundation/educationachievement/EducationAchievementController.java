package kr.ac.knue.commonfoundation.educationachievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the education-achievement API while keeping authorization and request tracing at the HTTP boundary.
 */
@RestController
public class EducationAchievementController {
    private final EducationAchievementService service;

    public EducationAchievementController(EducationAchievementService service) {
        this.service = service;
    }

    /** Lists only the requested education-achievement type in the caller's permitted data scope. */
    @GetMapping("/api/business/education-achievements")
    public ApiResponse<EducationAchievementSearchResponse> listEducationAchievements(
            @RequestParam String achievementType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        CurrentUser user = requireEducationAchievementUser(request);
        return ApiResponse.ok(service.list(new EducationAchievementSearchCriteria(achievementType, page, size), user));
    }

    /** Saves a lecture-evaluation achievement and its required management-item value for the authenticated owner. */
    @PostMapping("/api/business/education-achievements")
    public ApiResponse<EducationAchievementRow> saveEducationAchievement(
            @Valid @RequestBody SaveEducationAchievementRequest payload,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireEducationAchievementUser(request);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.save(payload, user, effectiveRequestId), effectiveRequestId);
    }

    /** Applies a permitted workflow transition and appends its immutable status-history record. */
    @PostMapping("/api/business/education-achievements/{achievementId}/transition")
    public ApiResponse<EducationAchievementTransitionResponse> transitionEducationAchievement(
            @PathVariable Long achievementId,
            @Valid @RequestBody BusinessTransitionRequest payload,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireEducationAchievementUser(request);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.transition(achievementId, payload, user, effectiveRequestId), effectiveRequestId);
    }

    /** Logically deletes an R01-owned education achievement and returns its request-correlated audit summary. */
    @DeleteMapping("/api/business/education-achievements/{achievementId}")
    public ApiResponse<EducationAchievementDeletionResponse> deleteEducationAchievement(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireEducationAchievementUser(request);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.delete(achievementId, user, effectiveRequestId), effectiveRequestId);
    }

    /** Keeps server-side access control aligned with the OpenAPI R01/R02/R04 contract. */
    private CurrentUser requireEducationAchievementUser(HttpServletRequest request) {
        Object principal = request.getAttribute("currentUser");
        if (!(principal instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        if (List.of("R01", "R02", "R04").stream().noneMatch(user.roles()::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private String effectiveRequestId(String requestId) {
        return requestId != null && !requestId.isBlank() ? requestId.trim() : UUID.randomUUID().toString();
    }
}
