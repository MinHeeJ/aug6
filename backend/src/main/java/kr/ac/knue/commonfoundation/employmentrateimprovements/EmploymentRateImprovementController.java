package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Approved four-operation transport boundary; IDs for updates come only from the path. */
@RestController
@RequestMapping("/api/business/employment-rate-improvements")
public class EmploymentRateImprovementController {
    private final EmploymentRateImprovementService service;

    public EmploymentRateImprovementController(EmploymentRateImprovementService service) {
        this.service = service;
    }

    /** Reads a role-scoped, filtered page through the real persistence path. */
    @GetMapping
    public ApiResponse<EmploymentRateImprovementSearchResponse> listEmploymentRateImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            @RequestParam(required = false) String evaluationYear,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = currentUser(request, false);
        return ApiResponse.ok(service.list(new EmploymentRateImprovementSearchCriteria(
                page, pageSize, (long) page * pageSize, normalized(managementNo),
                normalized(managementItemCode), normalized(achievementStatus), normalized(evaluationYear)), user),
                trace(requestId));
    }

    /** Reads one row with the same ownership boundary as list. */
    @GetMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> getEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, currentUser(request, false)), trace(requestId));
    }

    /** Creates a new row; a body cannot change server identity or lifecycle fields. */
    @PostMapping
    public ApiResponse<EmploymentRateImprovementSaveResult> createEmploymentRateImprovement(
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = currentUser(request, true);
        String id = trace(requestId);
        return ApiResponse.ok(service.create(body, user, id), id);
    }

    /** Updates only the selected path ID while retaining its stored owner and evaluation year. */
    @PutMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResult> updateEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = currentUser(request, true);
        String id = trace(requestId);
        return ApiResponse.ok(service.update(achievementId, body, user, id), id);
    }

    private CurrentUser currentUser(HttpServletRequest request, boolean write) {
        CurrentUser user = request.getAttribute("currentUser") instanceof CurrentUser value ? value : null;
        EmploymentRateImprovementService.requireRole(user, write);
        return user;
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trace(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
    }
}
