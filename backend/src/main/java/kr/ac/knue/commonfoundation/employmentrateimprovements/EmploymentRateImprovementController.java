package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

/** HTTP boundary for the four approved employment-improvement operations. */
@RestController
@RequestMapping("/api/business/employment-rate-improvements")
public class EmploymentRateImprovementController {
    private final EmploymentRateImprovementService service;

    public EmploymentRateImprovementController(EmploymentRateImprovementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<EmploymentRateImprovementSearchResponse> listEmploymentRateImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(service.list(page, pageSize, managementItemCode,
                achievementStatus, user(request, false)), trace);
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> getEmploymentRateImprovement(
            @PathVariable Long achievementId, HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(service.get(achievementId, user(request, false)), trace);
    }

    @PostMapping
    public ApiResponse<EmploymentRateImprovementSaveResult> createEmploymentRateImprovement(
            @Valid @RequestBody EmploymentRateImprovementRequest body, HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(service.create(body, user(request, true), trace), trace);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResult> updateEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body, HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(service.update(achievementId, body, user(request, true), trace), trace);
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        CurrentUser user = request.getAttribute("currentUser") instanceof CurrentUser current ? current : null;
        EmploymentRateImprovementService.requireRole(user, write);
        return user;
    }

    static String trace(HttpServletRequest request) {
        if (request.getAttribute("employmentImprovementRequestId") instanceof String existing) return existing;
        String value = request.getHeader("X-Request-Id");
        String trace = value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
        if (trace.length() > 100) trace = UUID.randomUUID().toString();
        request.setAttribute("employmentImprovementRequestId", trace);
        return trace;
    }
}
