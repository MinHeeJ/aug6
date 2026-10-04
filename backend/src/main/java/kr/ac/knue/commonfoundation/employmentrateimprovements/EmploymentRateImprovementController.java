package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owns the four OpenAPI employment-rate improvement endpoints and enforces
 * reader/writer role boundaries before delegating data-scope policy to the service.
 */
@RestController
public class EmploymentRateImprovementController {
    private final EmploymentRateImprovementService service;

    public EmploymentRateImprovementController(EmploymentRateImprovementService service) {
        this.service = service;
    }

    /** Lists employment-rate improvements with the OpenAPI page and pageSize parameters. */
    @GetMapping("/api/business/employment-rate-improvements")
    public ApiResponse<EmploymentRateImprovementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(page, pageSize, requireReader(request)), traceId(requestId));
    }

    /** Returns one employment-rate improvement addressed by the OpenAPI achievementId path parameter. */
    @GetMapping("/api/business/employment-rate-improvements/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, requireReader(request)), traceId(requestId));
    }

    /** Creates a new employment-rate improvement using the R01-only service command. */
    @PostMapping("/api/business/employment-rate-improvements")
    public ApiResponse<EmploymentRateImprovementRow> create(
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.create(body, requireWriter(request), traceId), traceId);
    }

    /** Updates mutable fields on a non-finalized employment-rate improvement. */
    @PutMapping("/api/business/employment-rate-improvements/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, requireWriter(request), traceId), traceId);
    }

    private CurrentUser requireReader(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireWriter(HttpServletRequest request) {
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

    private String traceId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
