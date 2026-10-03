package kr.ac.knue.commonfoundation.employmentrateachievement;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the approved employment-rate achievement read API with request tracing. */
@RestController
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service) {
        this.service = service;
    }

    /** Lists records after validating pagination and resolving the authenticated caller. */
    @GetMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementSearchResponse> listEmploymentRateAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePagination(page, pageSize);
        return ApiResponse.ok(
                service.list(new EmploymentRateAchievementSearchCriteria(page, pageSize), requireReadUser(request)),
                effectiveRequestId(requestId));
    }

    /** Retrieves one record through exactly the same server-side data scope as the list. */
    @GetMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementRow> getEmploymentRateAchievement(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, requireReadUser(request)), effectiveRequestId(requestId));
    }

    private CurrentUser requireReadUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private void validatePagination(int page, int pageSize) {
        if (page < 0) {
            throw new BusinessValidationException(
                    "목록 페이지가 올바르지 않습니다.",
                    List.of(new ValidationError("page", "0 이상이어야 합니다.")));
        }
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
