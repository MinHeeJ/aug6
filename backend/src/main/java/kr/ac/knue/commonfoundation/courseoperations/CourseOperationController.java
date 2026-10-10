package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The four approved course operations; transport admission is rechecked by the service. */
@RestController
public class CourseOperationController {
    private final CourseOperationService service;

    public CourseOperationController(CourseOperationService service) {
        this.service = service;
    }

    @GetMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationSearchResponse> listCourseOperations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(new CourseOperationSearchCriteria(
                page, pageSize, 0L, managementNo, teacherName, managementItemCode, achievementStatus),
                user(request, false)), trace(request));
    }

    @GetMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationRow> getCourseOperation(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, false)), trace(request));
    }

    @PostMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationSaveResult> createCourseOperation(
            @Valid @RequestBody CourseOperationRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.create(body, user(request, true), trace(request)), trace(request));
    }

    @PutMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationSaveResult> updateCourseOperation(
            @PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.update(achievementId, body, user(request, true), trace(request)), trace(request));
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        CurrentUser user = (CurrentUser) request.getAttribute("currentUser");
        CourseOperationService.requireRole(user, write);
        return user;
    }

    private String trace(HttpServletRequest request) {
        return CourseOperationExceptionHandler.requestId(request);
    }
}
