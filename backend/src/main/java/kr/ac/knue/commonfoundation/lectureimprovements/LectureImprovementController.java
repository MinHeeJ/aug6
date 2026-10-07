package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

/** Approved lecture-improvement collection/create and selected-ID detail/update operations. */
@RestController
@RequestMapping("/api/business/lecture-improvements")
public class LectureImprovementController {
    private final LectureImprovementService service;

    public LectureImprovementController(LectureImprovementService service) {
        this.service = service;
    }

    /** Lists persistent caller-scoped achievements; pagination and filters also constrain the total. */
    @GetMapping
    public ApiResponse<LectureImprovementSearchResponse> listLectureImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = user(request, false);
        return ApiResponse.ok(service.list(new LectureImprovementSearchCriteria(
                page, pageSize, (long) Math.max(page, 0) * pageSize,
                clean(managementNo), clean(teacherName), clean(managementItemCode), clean(achievementStatus)), user),
                trace(requestId));
    }

    /** Resolves a selected record and rechecks its owner/organization/certification scope. */
    @GetMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> getLectureImprovement(@PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, false)), trace(requestId));
    }

    /** Creates a new DRAFT record, typed detail and audit histories in one transaction. */
    @PostMapping
    public ApiResponse<LectureImprovementSaveResult> createLectureImprovement(
            @Valid @RequestBody LectureImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String id = trace(requestId);
        return ApiResponse.ok(service.create(body, user(request, true), id), id);
    }

    /** Mutates only the selected editable record, preserving server-owned identity and year. */
    @PutMapping("/{achievementId}")
    public ApiResponse<LectureImprovementSaveResult> updateLectureImprovement(@PathVariable Long achievementId,
            @Valid @RequestBody LectureImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String id = trace(requestId);
        return ApiResponse.ok(service.update(achievementId, body, user(request, true), id), id);
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        CurrentUser user = request.getAttribute("currentUser") instanceof CurrentUser current ? current : null;
        LectureImprovementService.requireRole(user, write);
        return user;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trace(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
    }
}
