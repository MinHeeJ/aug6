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

/** Exposes the approved course-operation achievement HTTP contract. */
@RestController
public class CourseOperationController {
    private final CourseOperationService service;

    public CourseOperationController(CourseOperationService service) {
        this.service = service;
    }

    /** Lists caller-scoped rows using one of the contractually allowed page sizes. */
    @GetMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationListResponse> listCourseOperations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePage(page, pageSize);
        CurrentUser currentUser = requireReadRole(request);
        return ApiResponse.ok(service.list(page, pageSize, currentUser), effectiveRequestId(requestId));
    }

    /** Creates a course-operation achievement as the authenticated R01 faculty member. */
    @PostMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationSaveResponse> createCourseOperation(
            @Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser currentUser = requireWriteRole(request);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.create(body, currentUser, traceId), traceId);
    }

    /** Retrieves one course-operation achievement after service-level scope validation. */
    @GetMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationRow> getCourseOperation(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser currentUser = requireReadRole(request);
        return ApiResponse.ok(service.get(achievementId, currentUser), effectiveRequestId(requestId));
    }

    /** Updates an owned course-operation achievement through the guarded transaction. */
    @PutMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationSaveResponse> updateCourseOperation(
            @PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser currentUser = requireWriteRole(request);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, currentUser, traceId), traceId);
    }

    private CurrentUser requireReadRole(HttpServletRequest request) {
        CurrentUser currentUser = requireCurrentUser(request);
        if (currentUser.roles() == null
                || currentUser.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
        return currentUser;
    }

    private CurrentUser requireWriteRole(HttpServletRequest request) {
        CurrentUser currentUser = requireCurrentUser(request);
        if (currentUser.roles() == null || !currentUser.roles().contains("R01")) {
            throw new ForbiddenException();
        }
        return currentUser;
    }

    private CurrentUser requireCurrentUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser currentUser)) {
            throw new UnauthenticatedException();
        }
        return currentUser;
    }

    private void validatePage(int page, int pageSize) {
        if (page < 0) {
            throw new BusinessValidationException(
                    "페이지 번호가 올바르지 않습니다.",
                    List.of(new ValidationError("page", "0 이상의 값을 입력하세요.")));
        }
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
