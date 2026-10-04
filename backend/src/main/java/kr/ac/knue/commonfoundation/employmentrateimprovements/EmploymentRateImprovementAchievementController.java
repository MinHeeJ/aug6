package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP boundary for the approved BASIC-83 취업률 제고 list, detail, create, and
 * update operations; it preserves session-principal and request-id handling.
 */
@RestController
public class EmploymentRateImprovementAchievementController {
    private final EmploymentRateImprovementAchievementService service;

    public EmploymentRateImprovementAchievementController(
            EmploymentRateImprovementAchievementService service) {
        this.service = service;
    }

    /** Lists the current user's permitted 취업률 제고 achievements with approved paging sizes. */
    @GetMapping("/api/business/employment-rate-improvements")
    public ApiResponse<EmploymentRateImprovementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        CurrentUser currentUser = requireReadUser(request);
        return ApiResponse.ok(service.list(page, pageSize, currentUser), effectiveRequestId(requestId));
    }

    /** Returns an achievement detail only when the session principal has its matching data scope. */
    @GetMapping("/api/business/employment-rate-improvements/{achievementId}")
    public ApiResponse<EmploymentRateImprovementAchievementResponse> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser currentUser = requireReadUser(request);
        return ApiResponse.ok(service.get(achievementId, currentUser), effectiveRequestId(requestId));
    }

    /** Creates a DRAFT achievement through the service's guarded atomic write transaction. */
    @PostMapping("/api/business/employment-rate-improvements")
    public ApiResponse<EmploymentRateImprovementSaveResponse> create(
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser currentUser = requireWriteUser(request);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.create(body, currentUser, traceId), traceId);
    }

    /** Updates a selected achievement after the service preserves finalization and scope invariants. */
    @PutMapping("/api/business/employment-rate-improvements/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResponse> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser currentUser = requireWriteUser(request);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, currentUser, traceId), traceId);
    }

    private CurrentUser requireReadUser(HttpServletRequest request) {
        CurrentUser currentUser = currentUser(request);
        if (currentUser.roles() == null
                || currentUser.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
        return currentUser;
    }

    private CurrentUser requireWriteUser(HttpServletRequest request) {
        CurrentUser currentUser = currentUser(request);
        if (currentUser.roles() == null || !currentUser.roles().contains("R01")) {
            throw new ForbiddenException();
        }
        return currentUser;
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser currentUser)) {
            throw new UnauthenticatedException();
        }
        return currentUser;
    }

    private void validatePageSize(int pageSize) {
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
