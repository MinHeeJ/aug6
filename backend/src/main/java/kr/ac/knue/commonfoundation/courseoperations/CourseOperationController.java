package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

/** Approved course operations transport; delegates scope, period and transaction invariants to the service. */
@RestController
@RequestMapping("/api/business/course-operations")
public class CourseOperationController {
    private final CourseOperationService service;

    public CourseOperationController(CourseOperationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<CourseOperationSearchResponse> listCourseOperations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            @RequestParam(required = false) String evaluationYear,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(new CourseOperationSearchCriteria(
                page, pageSize, managementNo, managementItemCode, achievementStatus), user(request, false),
                evaluationYear), trace(requestId));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<CourseOperationRow> getCourseOperation(@PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, false)), trace(requestId));
    }

    @PostMapping
    public ApiResponse<CourseOperationSaveResult> createCourseOperation(@Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String id = trace(requestId);
        return ApiResponse.ok(service.create(body, user(request, true), id), id);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<CourseOperationSaveResult> updateCourseOperation(@PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String id = trace(requestId);
        return ApiResponse.ok(service.update(achievementId, body, user(request, true), id), id);
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        Object principal = request.getAttribute("currentUser");
        CurrentUser user = principal instanceof CurrentUser current ? current : null;
        CourseOperationService.requireRole(user, write);
        return user;
    }

    private String trace(String id) {
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
    }
}
