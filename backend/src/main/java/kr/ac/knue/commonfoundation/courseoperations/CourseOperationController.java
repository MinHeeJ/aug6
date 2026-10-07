package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.RequestIds;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Transport boundary for the four approved course-operation endpoints. */
@RestController
@RequestMapping("/api/business/course-operations")
public class CourseOperationController {
    private final CourseOperationService service;

    public CourseOperationController(CourseOperationService service) {
        this.service = service;
    }

    /** Lists only permitted rows and reports a count with the same scope and optional filters. */
    @GetMapping
    public ApiResponse<CourseOperationSearchResponse> listCourseOperations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        String trace = RequestIds.resolve(request);
        CurrentUser user = principal(request, false);
        if (page < 0 || !List.of(20, 50, 100).contains(pageSize)) {
            throw new BusinessValidationException("목록 조건을 확인하세요.",
                    List.of(new ValidationError("pageSize", "page는 0 이상, pageSize는 20/50/100이어야 합니다.")));
        }
        return ApiResponse.ok(service.list(new CourseOperationSearch(page, pageSize, (long) page * pageSize,
                normalized(managementItemCode), normalized(achievementStatus)), user), trace);
    }

    /** Retrieves an existing row through the same ownership boundary as the list. */
    @GetMapping("/{achievementId}")
    public ApiResponse<CourseOperationRow> getCourseOperation(
            @PathVariable Long achievementId, HttpServletRequest request) {
        String trace = RequestIds.resolve(request);
        return ApiResponse.ok(service.get(achievementId, principal(request, false)), trace);
    }

    /** Creates only; the payload cannot select or overwrite an existing achievement. */
    @PostMapping
    public ApiResponse<CourseOperationSaveResult> createCourseOperation(
            @Valid @RequestBody CourseOperationRequest body, HttpServletRequest request) {
        String trace = RequestIds.resolve(request);
        return ApiResponse.ok(service.create(body, principal(request, true), trace), trace);
    }

    /** Updates the URL-selected row without changing its teacher or evaluation-year identity. */
    @PutMapping("/{achievementId}")
    public ApiResponse<CourseOperationSaveResult> updateCourseOperation(
            @PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationRequest body,
            HttpServletRequest request) {
        String trace = RequestIds.resolve(request);
        return ApiResponse.ok(service.update(achievementId, body, principal(request, true), trace), trace);
    }

    private CurrentUser principal(HttpServletRequest request, boolean write) {
        if (!(request.getAttribute("currentUser") instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        List<String> roles = write ? List.of("R01", "R09") : List.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(roles::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
