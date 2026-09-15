package kr.ac.knue.commonfoundation.basic65;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

@RestController
public class TeachingEvaluationAchievementController {
    private final TeachingEvaluationAchievementService service;

    public TeachingEvaluationAchievementController(TeachingEvaluationAchievementService service) {
        this.service = service;
    }

    @GetMapping("/api/business/teaching-evaluation-achievements")
    public ApiResponse<TeachingEvaluationAchievementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String courseKeyword,
            @RequestParam(required = false) String evaluationStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireAchievementReader(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.list(new TeachingEvaluationAchievementSearchCriteria(
                page, size, evaluationYear, academicYear, semester, courseKeyword, evaluationStatus), user.userId()), effectiveRequestId);
    }

    @PostMapping("/api/business/teaching-evaluation-achievements")
    public ApiResponse<TeachingEvaluationAchievementRow> save(
            @Valid @RequestBody SaveTeachingEvaluationAchievementRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireFacultyWriter(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.save(request, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireAchievementReader(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles().stream().anyMatch(role -> role.equals("R01") || role.equals("R02") || role.equals("R04"))) return user;
        throw new ForbiddenException();
    }

    private CurrentUser requireFacultyWriter(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles().contains("R01")) return user;
        throw new ForbiddenException();
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) return currentUser;
        throw new UnauthenticatedException();
    }

    private String effectiveRequestId(String requestId) {
        return requestId != null && !requestId.isBlank() ? requestId.trim() : UUID.randomUUID().toString();
    }
}
