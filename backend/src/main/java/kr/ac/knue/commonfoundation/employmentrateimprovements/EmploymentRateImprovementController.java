package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.RequestIds;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Approved four operations; the service enforces roles, data scope and atomic write invariants. */
@RestController
@RequestMapping("/api/business/employment-rate-improvements")
public class EmploymentRateImprovementController {
    private final EmploymentRateImprovementService service;

    public EmploymentRateImprovementController(EmploymentRateImprovementService service) {
        this.service = service;
    }

    /** Lists the authenticated user's visible records and matching total. */
    @GetMapping
    public ApiResponse<EmploymentRateImprovementSearchResponse> listEmploymentRateImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String certificationStatus,
            HttpServletRequest request) {
        String trace = RequestIds.resolve(request);
        return ApiResponse.ok(service.list(page, pageSize, managementNo, managementItemCode,
                certificationStatus, user(request)), trace);
    }

    /** Resolves the selected domain identity, not a sample or inferred record. */
    @GetMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> getEmploymentRateImprovement(
            @PathVariable Long achievementId, HttpServletRequest request) {
        String trace = RequestIds.resolve(request);
        return ApiResponse.ok(service.get(achievementId, user(request)), trace);
    }

    /** Creates a new source row and its history in one transaction. */
    @PostMapping
    public ApiResponse<EmploymentRateImprovementSaveResult> createEmploymentRateImprovement(
            @Valid @RequestBody EmploymentRateImprovementRequest body, HttpServletRequest request) {
        String trace = RequestIds.resolve(request);
        return ApiResponse.ok(service.create(body, user(request), trace), trace);
    }

    /** Updates only the existing path identity and leaves its owner/evaluation year unchanged. */
    @PutMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResult> updateEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            HttpServletRequest request) {
        String trace = RequestIds.resolve(request);
        return ApiResponse.ok(service.update(achievementId, body, user(request), trace), trace);
    }

    private CurrentUser user(HttpServletRequest request) {
        if (request.getAttribute("currentUser") instanceof CurrentUser user) {
            return user;
        }
        throw new UnauthenticatedException();
    }
}
