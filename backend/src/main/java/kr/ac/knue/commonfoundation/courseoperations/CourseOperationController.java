package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP boundary for the four approved course-operation endpoints; lifecycle commands remain elsewhere. */
@RestController
@RequestMapping("/api/business/course-operations")
public class CourseOperationController {
    private final CourseOperationService service;

    public CourseOperationController(CourseOperationService service) {
        this.service = service;
    }

    /** Prepares one correlation identifier before binding/validation so failure and success use the same value. */
    @ModelAttribute
    public void requestId(HttpServletRequest request) {
        String id = request.getHeader("X-Request-Id");
        request.setAttribute("courseOperationRequestId",
                id != null && id.matches("[A-Za-z0-9._:-]{1,100}") ? id : UUID.randomUUID().toString());
    }

    /** Lists scoped results with validated pagination and optional search predicates. */
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
                page, pageSize, managementNo, teacherName, managementItemCode, achievementStatus), user), id(request));
    }

    /** Reads a typed record and rechecks data scope in the application boundary. */
    @GetMapping("/{achievementId}")
    public ApiResponse<CourseOperationRow> getCourseOperation(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, false)), id(request));
    }

    /** Creates a caller-owned draft, never an upsert based on an editable body identifier. */
    @PostMapping
    public ApiResponse<CourseOperationSaveResult> createCourseOperation(
            @Valid @RequestBody CourseOperationRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.create(body, user(request, true), id(request)), id(request));
    }

    /** Updates only the selected path identity without permitting owner/year/status replacement. */
    @PutMapping("/{achievementId}")
    public ApiResponse<CourseOperationSaveResult> updateCourseOperation(
            @PathVariable Long achievementId,
            @Valid @RequestBody CourseOperationRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(service.update(achievementId, body, user(request, true), id(request)), id(request));
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        Object value = request.getAttribute("currentUser");
        CurrentUser user = value instanceof CurrentUser current ? current : null;
        CourseOperationService.requireRole(user, write);
        return user;
    }

    static String id(HttpServletRequest request) {
        Object value = request.getAttribute("courseOperationRequestId");
        return value == null ? UUID.randomUUID().toString() : value.toString();
    }
}
