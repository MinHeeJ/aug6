package kr.ac.knue.commonfoundation.faculty.achievement;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP boundary for authorized faculty degree-completion search and save requests. It deliberately
 * passes the authenticated employee identity to the service rather than accepting it from clients.
 */
@RestController
public class DegreeCompletionAchievementController {
    private final DegreeCompletionAchievementService service;

    public DegreeCompletionAchievementController(DegreeCompletionAchievementService service) {
        this.service = service;
    }

    /** Lists visible degree-completion achievements with OpenAPI pagination and optional filters. */
    @GetMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionSearchResponse> listDegreeCompletionAchievements(
            HttpServletRequest servletRequest,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String certificationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        requireAuthorizedUser(servletRequest);
        return ApiResponse.ok(service.list(new DegreeCompletionSearchCriteria(managementNo, teacherName, certificationStatus, page, pageSize)), effectiveRequestId(requestId));
    }

    /** Saves the header and repeated degree-student details for an authorized education user. */
    @PostMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionAchievementRow> saveDegreeCompletionAchievement(
            @RequestBody SaveDegreeCompletionAchievementRequest request,
            HttpServletRequest servletRequest,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        CurrentUser user = requireAuthorizedUser(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.save(request, user.userId(), user.employeeNo(), effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireAuthorizedUser(HttpServletRequest servletRequest) {
        Object user = servletRequest.getAttribute("currentUser");
        if (!(user instanceof CurrentUser currentUser)) throw new UnauthenticatedException();
        if (currentUser.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) throw new ForbiddenException();
        return currentUser;
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
