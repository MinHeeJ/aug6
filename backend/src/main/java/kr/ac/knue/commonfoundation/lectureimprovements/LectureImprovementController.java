package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

/** Thin transport boundary for the four approved teaching-improvement operations. */
@RestController
@RequestMapping("/api/business/lecture-improvements")
public class LectureImprovementController {
    private final LectureImprovementService service;

    public LectureImprovementController(LectureImprovementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<LectureImprovementSearchResponse> listLectureImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String semester,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(new LectureImprovementSearchCriteria(
                page, pageSize, (long) page * pageSize,
                normalized(managementItemCode), normalized(academicYear), normalized(semester)), user(request, false)),
                trace(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<LectureImprovementRow> getLectureImprovement(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, false)), trace(request));
    }

    @PostMapping
    public ApiResponse<LectureImprovementSaveResult> createLectureImprovement(
            @Valid @RequestBody LectureImprovementRequest body, HttpServletRequest request) {
        String requestId = trace(request);
        return ApiResponse.ok(service.save(null, body, user(request, true), requestId), requestId);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<LectureImprovementSaveResult> updateLectureImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureImprovementRequest body, HttpServletRequest request) {
        String requestId = trace(request);
        return ApiResponse.ok(service.save(achievementId, body, user(request, true), requestId), requestId);
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        CurrentUser user = request.getAttribute("currentUser") instanceof CurrentUser current ? current : null;
        LectureImprovementService.requireRole(user, write);
        return user;
    }

    static String trace(HttpServletRequest request) {
        if (request.getAttribute("lectureImprovementRequestId") instanceof String id) return id;
        String id = request.getHeader("X-Request-Id");
        if (id == null || id.isBlank() || id.length() > 100) id = UUID.randomUUID().toString();
        request.setAttribute("lectureImprovementRequestId", id);
        return id;
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
