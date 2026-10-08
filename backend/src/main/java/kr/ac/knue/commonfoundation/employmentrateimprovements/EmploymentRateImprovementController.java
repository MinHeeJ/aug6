package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
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
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String achievementStatus,
            @RequestParam(required = false) String teacherName,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(new EmploymentRateImprovementSearchCriteria(
                page, pageSize, 0, managementItemCode, evaluationYear, achievementStatus, teacherName),
                currentUser(request)), trace(requestId));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> getEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, currentUser(request)), trace(requestId));
    }

    @PostMapping
    public ApiResponse<EmploymentRateImprovementSaveResult> createEmploymentRateImprovement(
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String id = trace(requestId);
        return ApiResponse.ok(service.create(body, currentUser(request), id), id);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResult> updateEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String id = trace(requestId);
        return ApiResponse.ok(service.update(achievementId, body, currentUser(request), id), id);
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        if (!(request.getAttribute("currentUser") instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        return user;
    }

    private String trace(String id) {
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
    }
}
