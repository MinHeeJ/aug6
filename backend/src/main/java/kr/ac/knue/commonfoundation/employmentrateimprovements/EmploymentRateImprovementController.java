package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.web.bind.annotation.*;

/** Session-authenticated transport for the four approved employment-rate-improvement operations. */
@RestController
@RequestMapping("/api/business/employment-rate-improvements")
public class EmploymentRateImprovementController {
    private final EmploymentRateImprovementService service;

    public EmploymentRateImprovementController(EmploymentRateImprovementService service) {
        this.service = service;
    }

    @ModelAttribute
    public void requestId(HttpServletRequest request) {
        if (request.getAttribute("requestId") == null) {
            String header = request.getHeader("X-Request-Id");
            request.setAttribute("requestId",
                    header == null || header.isBlank() ? UUID.randomUUID().toString() : header);
        }
    }

    @GetMapping
    public ApiResponse<EmploymentRateImprovementSearchResponse> listEmploymentRateImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        CurrentUser user = user(request, false);
        if (page < 0 || page > Integer.MAX_VALUE / 100 || !List.of(20, 50, 100).contains(pageSize)) {
            throw new BusinessValidationException("목록 조건을 확인하세요.",
                    List.of(new ValidationError("pageSize", "page는 0 이상, pageSize는 20/50/100이어야 합니다.")));
        }
        return ApiResponse.ok(service.list(new EmploymentRateImprovementSearch(
                page, pageSize, page * pageSize, managementNo, teacherName,
                managementItemCode, achievementStatus), user),
                trace(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> getEmploymentRateImprovement(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, false)), trace(request));
    }

    @PostMapping
    public ApiResponse<EmploymentRateImprovementSaveResult> createEmploymentRateImprovement(
            @Valid @RequestBody EmploymentRateImprovementRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.create(body, user(request, true), trace(request)), trace(request));
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResult> updateEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.update(achievementId, body, user(request, true), trace(request)), trace(request));
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        CurrentUser user = request.getAttribute("currentUser") instanceof CurrentUser current ? current : null;
        EmploymentRateImprovementService.requireRole(user, write);
        return user;
    }

    private String trace(HttpServletRequest request) {
        return (String) request.getAttribute("requestId");
    }
}
