package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the Course Operations API while enforcing its operation-specific role contract. */
@RestController
public class CourseOperationController {
    private final CourseOperationService service;

    public CourseOperationController(CourseOperationService service) {
        this.service = service;
    }

    /** Lists course-operation achievements visible to R01, R02, or R04 within their data scope. */
    @GetMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationSearchResponse> listCourseOperations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        CurrentUser user = requireRole(request, List.of("R01", "R02", "R04"));
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(
                service.list(new CourseOperationSearchCriteria(page, pageSize), user),
                traceId);
    }

    /** Returns a single visible course-operation achievement and its performance detail. */
    @GetMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationRow> getCourseOperation(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireRole(request, List.of("R01", "R02", "R04"));
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.get(achievementId, user), traceId);
    }

    /** Creates a self-owned course-operation achievement through the atomic service transaction. */
    @PostMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationRow> createCourseOperation(
            @Valid @RequestBody CourseOperationSaveRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireRole(request, List.of("R01"));
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.create(body, user, traceId), traceId);
    }

    /** Updates a self-owned mutable course-operation achievement identified only by the path. */
    @PutMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationRow> updateCourseOperation(
            @PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationSaveRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireRole(request, List.of("R01"));
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, user, traceId), traceId);
    }

    private CurrentUser requireRole(HttpServletRequest request, List<String> allowedRoles) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || user.roles().stream().noneMatch(allowedRoles::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private void validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
