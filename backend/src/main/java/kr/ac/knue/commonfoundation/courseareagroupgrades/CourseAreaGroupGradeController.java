package kr.ac.knue.commonfoundation.courseareagroupgrades;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP boundary for the read-only course-area group grade list and its Excel-compatible export.
 */
@RestController
public class CourseAreaGroupGradeController {
    private static final Set<String> ALLOWED_ROLES = Set.of("R01", "R04", "R09");
    private final CourseAreaGroupGradeService service;

    public CourseAreaGroupGradeController(CourseAreaGroupGradeService service) {
        this.service = service;
    }

    /**
     * Lists published group grades after server-side condition validation and enforcement of the R01 self scope.
     */
    @GetMapping("/api/faculty/course-area-group-grades")
    public ApiResponse<CourseAreaGroupGradeSearchResponse> listCourseAreaGroupGrades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String completionTypeCode,
            @RequestParam(required = false) String semesterCode,
            @RequestParam(required = false) String courseAreaCode,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireGradeReadPermission(request);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(
                service.list(page, pageSize, completionTypeCode, semesterCode, courseAreaCode, null, user, effectiveRequestId),
                effectiveRequestId);
    }

    /**
     * Downloads the permitted result set as an Excel workbook.
     */
    @GetMapping("/api/faculty/course-area-group-grades/download")
    public ResponseEntity<byte[]> downloadCourseAreaGroupGrades(
            @RequestParam(required = false) String completionTypeCode,
            @RequestParam(required = false) String semesterCode,
            @RequestParam(required = false) String courseAreaCode,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireGradeReadPermission(request);
        String effectiveRequestId = effectiveRequestId(requestId);
        byte[] body = service.download(
                0,
                100,
                completionTypeCode,
                semesterCode,
                courseAreaCode,
                null,
                user,
                effectiveRequestId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header("X-Request-Id", effectiveRequestId)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("course-area-group-grades.xlsx")
                        .build()
                        .toString())
                .body(body);
    }

    private CurrentUser requireGradeReadPermission(HttpServletRequest request) {
        Object principal = request.getAttribute("currentUser");
        if (!(principal instanceof CurrentUser currentUser)) {
            throw new UnauthenticatedException();
        }
        if (currentUser.roles().stream().noneMatch(ALLOWED_ROLES::contains)) {
            throw new ForbiddenException();
        }
        return currentUser;
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
