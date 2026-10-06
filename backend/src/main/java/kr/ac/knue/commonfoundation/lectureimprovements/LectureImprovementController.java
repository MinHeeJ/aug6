package kr.ac.knue.commonfoundation.lectureimprovements;

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

/** Approved four FR-031 HTTP operations; session establishment remains in AuthenticationFilter. */
@RestController
@RequestMapping("/api/business/lecture-improvements")
public class LectureImprovementController {
    private final LectureImprovementService service;

    public LectureImprovementController(LectureImprovementService service) {
        this.service = service;
    }

    /** Lists the authorized union of owner, department and certification scope with DB choices. */
    @GetMapping
    public ApiResponse<LectureImprovementSearchResponse> listLectureImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.list(new LectureImprovementSearchCriteria(
                page,
                pageSize,
                (long) Math.max(0, page) * pageSize,
                normalized(managementNo),
                normalized(teacherName),
                normalized(managementItemCode),
                normalized(achievementStatus)), principal(servletRequest)), trace(requestId));
    }

    /** Fetches the selected path identity without trusting client-supplied owner/status. */
    @GetMapping("/{achievementId}")
    public ApiResponse<LectureImprovementRow> getLectureImprovement(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.get(achievementId, principal(servletRequest)), trace(requestId));
    }

    /** Creates only for the authenticated R01 owner; 200 carries the persisted view and date warning. */
    @PostMapping
    public ApiResponse<LectureImprovementSaveResult> createLectureImprovement(
            @Valid @RequestBody LectureImprovementRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        String trace = trace(requestId);
        return ApiResponse.ok(service.create(request, principal(servletRequest), trace), trace);
    }

    /** Updates only the path row through the service's atomic lock/period/ownership boundary. */
    @PutMapping("/{achievementId}")
    public ApiResponse<LectureImprovementSaveResult> updateLectureImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureImprovementRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        String trace = trace(requestId);
        return ApiResponse.ok(service.update(achievementId, request, principal(servletRequest), trace), trace);
    }

    private CurrentUser principal(HttpServletRequest request) {
        if (!(request.getAttribute("currentUser") instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        return user;
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trace(String value) {
        return value == null || value.isBlank() || value.length() > 100 ? UUID.randomUUID().toString() : value.trim();
    }
}
