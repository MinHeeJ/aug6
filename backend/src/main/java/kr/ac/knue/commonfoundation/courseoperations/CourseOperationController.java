package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thin HTTP boundary for the four approved FR-030 operations, using the existing session principal. */
@RestController
@RequestMapping("/api/business/course-operations")
public class CourseOperationController {
    private final CourseOperationService service;

    public CourseOperationController(CourseOperationService service) {
        this.service = service;
    }

    /** Returns a caller-scoped list with matching total and request correlation. */
    @GetMapping
    public ApiResponse<CourseOperationSearchResponse> listCourseOperations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(new CourseOperationSearchCriteria(
                page, pageSize, 0, managementNo, teacherName, managementItemCode, achievementStatus), user(request)),
                trace(requestId));
    }

    /** Direct detail lookup is protected by the same data scope as list. */
    @GetMapping("/{achievementId}")
    public ApiResponse<CourseOperationRow> getCourseOperation(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request)), trace(requestId));
    }

    /** Creates a new self-owned DRAFT; this collection POST never updates existing rows. */
    @PostMapping
    public ApiResponse<CourseOperationSaveResult> createCourseOperation(
            @Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String id = trace(requestId);
        return ApiResponse.ok(service.create(body, user(request), id), id);
    }

    /** Updates only the path-selected row, leaving owner, year and lifecycle immutable. */
    @PutMapping("/{achievementId}")
    public ApiResponse<CourseOperationSaveResult> updateCourseOperation(
            @PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String id = trace(requestId);
        return ApiResponse.ok(service.update(achievementId, body, user(request), id), id);
    }

    private CurrentUser user(HttpServletRequest request) {
        if (!(request.getAttribute("currentUser") instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        return user;
    }

    private String trace(String value) {
        return value == null || value.isBlank() || value.length() > 100 ? UUID.randomUUID().toString() : value.trim();
    }
}
