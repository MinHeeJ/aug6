package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementRequestFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Approved employment-improvement transport; never accepts caller-supplied ownership or lifecycle fields. */
@RestController
@RequestMapping("/api/business/employment-rate-improvements")
public class EmploymentRateImprovementController {
    private final EmploymentRateImprovementService service;

    public EmploymentRateImprovementController(EmploymentRateImprovementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listEmploymentRateImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String certificationStatus,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(new EmploymentRateImprovementSearchCriteria(
                page, pageSize, (long) page * pageSize, normalized(managementNo), normalized(teacherName),
                normalized(managementItemCode), normalized(certificationStatus)), user(request, false)), id(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> getEmploymentRateImprovement(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, false)), id(request));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createEmploymentRateImprovement(
            @Valid @RequestBody EmploymentRateImprovementRequest body, HttpServletRequest request) {
        String requestId = id(request);
        return ApiResponse.ok(service.create(body, user(request, true), requestId), requestId);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> updateEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body, HttpServletRequest request) {
        String requestId = id(request);
        return ApiResponse.ok(service.update(achievementId, body, user(request, true), requestId), requestId);
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        CurrentUser user = request.getAttribute("currentUser") instanceof CurrentUser current ? current : null;
        EmploymentRateImprovementService.requireRole(user, write);
        return user;
    }

    private String id(HttpServletRequest request) {
        Object id = request.getAttribute(EducationAchievementRequestFilter.REQUEST_ID_ATTRIBUTE);
        if (id instanceof String value) {
            return value;
        }
        String header = request.getHeader("X-Request-Id");
        String value = header != null && header.matches("[A-Za-z0-9._:-]{1,100}")
                ? header : UUID.randomUUID().toString();
        request.setAttribute(EducationAchievementRequestFilter.REQUEST_ID_ATTRIBUTE, value);
        return value;
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
