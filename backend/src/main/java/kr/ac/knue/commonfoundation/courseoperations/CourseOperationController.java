package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.*;

/** Four approved course operation endpoints; no implicit upsert, delete or status mutation. */
@RestController
public class CourseOperationController {
    private final CourseOperationService service;

    public CourseOperationController(CourseOperationService service) {
        this.service = service;
    }

    /** Lists scoped course operation details with normalized pagination. */
    @GetMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationSearchResponse> listCourseOperations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String achievementStatus,
            @RequestParam(required = false) String teacherName,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(new CourseOperationSearchCriteria(
                page, pageSize, 0, managementItemCode, evaluationYear, achievementStatus, teacherName),
                user(request, false)), trace(requestId));
    }

    /** Retrieves the selected domain ID after server-side scope checks. */
    @GetMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationRow> getCourseOperation(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, false)), trace(requestId));
    }

    /** Creates a header, detail and histories in one service transaction. */
    @PostMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationSaveResult> createCourseOperation(
            @Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String id = trace(requestId);
        return ApiResponse.ok(service.create(body, user(request, true), id), id);
    }

    /** Updates only the path-selected row while retaining its year and lifecycle identity. */
    @PutMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationSaveResult> updateCourseOperation(
            @PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String id = trace(requestId);
        return ApiResponse.ok(service.update(achievementId, body, user(request, true), id), id);
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        if (!(request.getAttribute("currentUser") instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        List<String> roles = write ? List.of("R01", "R09") : List.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(roles::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private String trace(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
    }
}
