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

/** Exposes the approved 취업률 제고 list, detail, create, and update operations. */
@RestController
public class EmploymentRateImprovementController {
    private final EmploymentRateImprovementService service;

    public EmploymentRateImprovementController(EmploymentRateImprovementService service) {
        this.service = service;
    }

    /** Lists caller-scoped employment-rate-improvement achievements with approved page sizes. */
    @GetMapping("/api/business/employment-rate-improvements")
    public ApiResponse<EmploymentRateImprovementSearchResponse> listEmploymentRateImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        return ApiResponse.ok(
                service.list(new EmploymentRateImprovementSearchCriteria(page, pageSize), requireReadUser(request)),
                effectiveRequestId(requestId));
    }

    /** Returns an individual caller-permitted employment-rate-improvement achievement. */
    @GetMapping("/api/business/employment-rate-improvements/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> getEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, requireReadUser(request)), effectiveRequestId(requestId));
    }

    /** Creates a draft achievement through the service's period, lock, and audit transaction. */
    @PostMapping("/api/business/employment-rate-improvements")
    public ApiResponse<EmploymentRateImprovementSaveResult> createEmploymentRateImprovement(
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.create(body, requireWriteUser(request)), effectiveRequestId(requestId));
    }

    /** Updates an owned achievement only when its evaluation data remains mutable. */
    @PutMapping("/api/business/employment-rate-improvements/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResult> updateEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(
                service.update(achievementId, body, requireWriteUser(request)),
                effectiveRequestId(requestId));
    }

    private CurrentUser requireReadUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || user.roles().stream().noneMatch(List.of("R01", "R02", "R04")::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireWriteUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R01")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        return user;
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
