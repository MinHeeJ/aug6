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

/** Exposes the approved 취업률 제고 list, detail, create, and update HTTP operations. */
@RestController
public class EmploymentRateImprovementController {
    private final EmploymentRateImprovementService service;

    public EmploymentRateImprovementController(EmploymentRateImprovementService service) {
        this.service = service;
    }

    /** Lists caller-visible achievements with the approved 20/50/100 page-size constraint. */
    @GetMapping("/api/business/employment-rate-improvements")
    public ApiResponse<EmploymentRateImprovementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(
                service.list(new EmploymentRateImprovementSearchCriteria(page, pageSize), currentUser(request)),
                traceId);
    }

    /** Reads one caller-visible achievement. */
    @GetMapping("/api/business/employment-rate-improvements/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.get(achievementId, currentUser(request)), traceId);
    }

    /** Creates a DRAFT achievement for an R01 faculty member. */
    @PostMapping("/api/business/employment-rate-improvements")
    public ApiResponse<EmploymentRateImprovementSaveResult> create(
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser requester = currentUser(request);
        requireWriteRole(requester);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.create(body, requester, traceId), traceId);
    }

    /** Updates an R01-owned achievement after server-side guard validation. */
    @PutMapping("/api/business/employment-rate-improvements/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResult> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, currentUser(request), traceId), traceId);
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        return user;
    }

    private void requireWriteRole(CurrentUser requester) {
        if (requester.roles() == null || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
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
