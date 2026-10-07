package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

/** Approved course-operation transport; service owns scope, locks, period and persistence rules. */
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
                page, pageSize, 0, managementNo, teacherName, managementItemCode, achievementStatus), user), requestId());
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<CourseOperationRow> getCourseOperation(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, false)), requestId());
    }

    @PostMapping
    public ApiResponse<CourseOperationSaveResult> createCourseOperation(
            @Valid @RequestBody CourseOperationRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.create(body, user(request, true), requestId()), requestId());
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<CourseOperationSaveResult> updateCourseOperation(
            @PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(service.update(achievementId, body, user(request, true), requestId()), requestId());
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        CurrentUser user = request.getAttribute("currentUser") instanceof CurrentUser current ? current : null;
        CourseOperationService.requireRole(user, write);
        return user;
    }

    private String requestId() {
        return EducationAchievementRequestFilter.currentRequestId();
    }
}
