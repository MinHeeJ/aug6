package kr.ac.knue.commonfoundation.courseoperation;

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

/** Exposes the approved course-operation list, detail, create, and update API contract. */
@RestController
public class CourseOperationController {
    private final CourseOperationService service;

    public CourseOperationController(CourseOperationService service) {
        this.service = service;
    }

    /** Lists course-operation achievements visible through the authenticated caller's data scope. */
    @GetMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.list(page, pageSize, readableUser(request)), traceId);
    }

    /** Returns one course-operation achievement after the same data-scope enforcement as the list. */
    @GetMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationResponse> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.get(achievementId, readableUser(request)), traceId);
    }

    /** Creates a course-operation achievement for the authenticated R01 faculty member. */
    @PostMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationResponse> create(
            @Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.create(body, writerUser(request), traceId), traceId);
    }

    /** Updates an existing caller-owned course-operation achievement through the guarded service. */
    @PutMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationResponse> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, writerUser(request), traceId), traceId);
    }

    private CurrentUser readableUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser writerUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R01")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
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

    private String traceId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
