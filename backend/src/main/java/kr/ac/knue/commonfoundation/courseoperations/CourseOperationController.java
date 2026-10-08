package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

/** Four approved course-operation endpoints, authenticated by the existing session filter. */
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
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        CurrentUser user = user(request, false);
        return ApiResponse.ok(service.list(new CourseOperationSearchCriteria(
                page, pageSize, 0, managementNo, teacherName, managementItemCode, achievementStatus), user),
                trace(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<CourseOperationRow> getCourseOperation(@PathVariable Long achievementId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, false)), trace(request));
    }

    @PostMapping
    public ApiResponse<CourseOperationSaveResult> createCourseOperation(
            @Valid @RequestBody CourseOperationRequest body, HttpServletRequest request) {
        CurrentUser user = user(request, true);
        String requestId = trace(request);
        return ApiResponse.ok(service.save(null, body, user, requestId), requestId);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<CourseOperationSaveResult> updateCourseOperation(@PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationRequest body, HttpServletRequest request) {
        CurrentUser user = user(request, true);
        String requestId = trace(request);
        return ApiResponse.ok(service.save(achievementId, body, user, requestId), requestId);
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        Object principal = request.getAttribute("currentUser");
        CurrentUser user = principal instanceof CurrentUser current ? current : null;
        CourseOperationService.requireRole(user, write);
        return user;
    }

    static String trace(HttpServletRequest request) {
        Object existing = request.getAttribute("courseOperationRequestId");
        if (existing != null) return existing.toString();
        String header = request.getHeader("X-Request-Id");
        String id = header == null || header.isBlank() ? UUID.randomUUID().toString() : header.trim();
        request.setAttribute("courseOperationRequestId", id);
        return id;
    }
}
