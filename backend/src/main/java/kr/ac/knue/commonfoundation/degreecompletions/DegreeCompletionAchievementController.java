package kr.ac.knue.commonfoundation.degreecompletions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.RequestIdentifierFilter;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the OpenAPI contract for degree-completion search and atomic header/detail saves.
 * Authorization and lifecycle guards remain in the service so direct API access is protected too.
 */
@RestController
public class DegreeCompletionAchievementController {
    private final DegreeCompletionAchievementService service;

    public DegreeCompletionAchievementController(DegreeCompletionAchievementService service) { this.service = service; }

    /** Lists only persisted, non-deleted degree-completion achievements that the caller may access. */
    @GetMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionAchievementSearchResponse> listDegreeCompletionAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String certificationStatus,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.list(new DegreeCompletionAchievementSearchCriteria(managementNo, teacherName,
                certificationStatus), page, pageSize, currentUser(servletRequest)), RequestIdentifierFilter.requestId(servletRequest));
    }

    /** Saves a degree-completion header and all submitted student rows in one transaction. */
    @PostMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionAchievementRow> saveDegreeCompletionAchievement(
            @Valid @RequestBody DegreeCompletionAchievementSaveRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.save(request, currentUser(servletRequest)), RequestIdentifierFilter.requestId(servletRequest));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) return currentUser;
        throw new UnauthenticatedException();
    }
}
