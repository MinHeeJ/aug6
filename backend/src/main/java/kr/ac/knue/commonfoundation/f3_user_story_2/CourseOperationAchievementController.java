package kr.ac.knue.commonfoundation.f3_user_story_2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.RequestIdResolver;
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

/** Exposes the approved course-operation list, detail, create, and update HTTP contract. */
@RestController
public class CourseOperationAchievementController {
    private final CourseOperationAchievementService service;

    public CourseOperationAchievementController(CourseOperationAchievementService service) {
        this.service = service;
    }

    /** Lists course-operation achievements with optional, persistence-safe search filters. */
    @GetMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationSearchResponse> listCourseOperations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String certificationStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        String resolvedRequestId = RequestIdResolver.resolve(requestId);
        return ApiResponse.ok(
                service.list(
                        new CourseOperationSearchCriteria(
                                page,
                                pageSize,
                                managementNo,
                                teacherName,
                                managementItemCode,
                                certificationStatus),
                        requireCurrentUser(request)),
                resolvedRequestId);
    }

    /** Retrieves one course-operation achievement after service-level data-scope verification. */
    @GetMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationResponse> getCourseOperation(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(
                service.get(achievementId, requireCurrentUser(request)),
                RequestIdResolver.resolve(requestId));
    }

    /** Creates a course-operation achievement as an R01 faculty user. */
    @PostMapping("/api/business/course-operations")
    public ApiResponse<CourseOperationResponse> createCourseOperation(
            @Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser currentUser = requireWriteRole(request);
        String resolvedRequestId = RequestIdResolver.resolve(requestId);
        return ApiResponse.ok(
                service.create(body, currentUser, resolvedRequestId),
                resolvedRequestId);
    }

    /** Updates an existing editable course-operation achievement as its R01 owner. */
    @PutMapping("/api/business/course-operations/{achievementId}")
    public ApiResponse<CourseOperationResponse> updateCourseOperation(
            @PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String resolvedRequestId = RequestIdResolver.resolve(requestId);
        return ApiResponse.ok(
                service.update(achievementId, body, requireCurrentUser(request), resolvedRequestId),
                resolvedRequestId);
    }

    private CurrentUser requireCurrentUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (current instanceof CurrentUser user) {
            return user;
        }
        throw new UnauthenticatedException();
    }

    private CurrentUser requireWriteRole(HttpServletRequest request) {
        CurrentUser user = requireCurrentUser(request);
        if (user.roles() == null || !user.roles().contains("R01")) {
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
}
