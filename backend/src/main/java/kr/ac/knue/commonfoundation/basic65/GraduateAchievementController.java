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
public class GraduateAchievementController {
    private final GraduateAchievementService service;

    public GraduateAchievementController(GraduateAchievementService service) { this.service = service; }

    @GetMapping("/api/business/graduate-achievements")
    public ApiResponse<GraduateAchievementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String semester, @RequestParam(required = false) String studentKeyword,
            @RequestParam(required = false) String degreeType, @RequestParam(required = false) String achievementStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        CurrentUser user = requireReader(request);
        String effectiveRequestId = requestId(requestId);
        return ApiResponse.ok(service.list(new GraduateAchievementSearchCriteria(page, size, evaluationYear, academicYear, semester, studentKeyword, degreeType, achievementStatus), user.userId()), effectiveRequestId);
    }

    @PostMapping("/api/business/graduate-achievements")
    public ApiResponse<GraduateAchievementRow> save(@Valid @RequestBody SaveGraduateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (!user.roles().contains("R01")) throw new ForbiddenException();
        String effectiveRequestId = requestId(requestId);
        return ApiResponse.ok(service.save(body, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireReader(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles().stream().anyMatch(role -> role.equals("R01") || role.equals("R02") || role.equals("R04"))) return user;
        throw new ForbiddenException();
    }
    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) return currentUser;
        throw new UnauthenticatedException();
    }
    private String requestId(String requestId) { return requestId != null && !requestId.isBlank() ? requestId.trim() : UUID.randomUUID().toString(); }
}
