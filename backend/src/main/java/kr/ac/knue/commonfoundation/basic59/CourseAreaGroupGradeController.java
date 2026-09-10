package kr.ac.knue.commonfoundation.basic59;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourseAreaGroupGradeController {
    private static final Set<String> ALLOWED_ROLES = Set.of("R01", "R04");
    private final CourseAreaGroupGradeService service;

    public CourseAreaGroupGradeController(CourseAreaGroupGradeService service) {
        this.service = service;
    }

    @GetMapping("/api/business/course-area-group-grades")
    public ApiResponse<CourseAreaGroupGradeSearchResponse> listCourseAreaGroupGrades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20", name = "pageSize") int pageSize,
            @RequestParam(required = false) Long teacherUserId,
            @RequestParam(required = false) String completionTypeCode,
            @RequestParam(required = false) String semesterCode,
            @RequestParam(required = false) String courseAreaCode,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        validatePageSize(pageSize);
        String effectiveRequestId = effectiveRequestId(requestId);
        CurrentUser user = requireRole(currentUser(servletRequest));
        return ApiResponse.ok(service.list(new CourseAreaGroupGradeSearchCriteria(page, pageSize, teacherUserId, completionTypeCode, semesterCode, courseAreaCode), user, effectiveRequestId), effectiveRequestId);
    }

    @GetMapping("/api/business/course-area-group-grades/{gradeId}")
    public ApiResponse<CourseAreaGroupGradeRow> getCourseAreaGroupGradeDetail(
            @PathVariable Long gradeId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        String effectiveRequestId = effectiveRequestId(requestId);
        CurrentUser user = requireRole(currentUser(servletRequest));
        return ApiResponse.ok(service.getDetail(gradeId, user, effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) return currentUser;
        throw new UnauthenticatedException();
    }

    private CurrentUser requireRole(CurrentUser user) {
        if (user.roles().stream().anyMatch(ALLOWED_ROLES::contains)) return user;
        throw new ForbiddenException();
    }

    private void validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException("교과영역 그룹평가 성적 목록 표시 건수가 올바르지 않습니다.",
                    java.util.List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) return requestId.trim();
        return UUID.randomUUID().toString();
    }
}
