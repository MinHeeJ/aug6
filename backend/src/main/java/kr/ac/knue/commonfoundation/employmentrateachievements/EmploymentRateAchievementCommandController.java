package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.employmentrateachievement.EmploymentRateAchievementRequest;
import kr.ac.knue.commonfoundation.employmentrateachievement.EmploymentRateAchievementRow;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owns the create and update HTTP operations that complement the existing
 * employment-rate achievement read controller. Both operations require R01
 * because they mutate faculty-owned achievement data.
 */
@RestController
public class EmploymentRateAchievementCommandController {
    private final EmploymentRateAchievementCommandService service;

    public EmploymentRateAchievementCommandController(EmploymentRateAchievementCommandService service) {
        this.service = service;
    }

    /** Creates an employment-rate achievement for the authenticated R01 faculty member. */
    @PostMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementRow> createEmploymentRateAchievement(
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.create(body, requireWriter(request), traceId), traceId);
    }

    /** Updates only a caller-owned achievement after the guarded service checks mutable state. */
    @PutMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementRow> updateEmploymentRateAchievement(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, requireWriter(request), traceId), traceId);
    }

    private CurrentUser requireWriter(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || !user.roles().contains("R01")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
