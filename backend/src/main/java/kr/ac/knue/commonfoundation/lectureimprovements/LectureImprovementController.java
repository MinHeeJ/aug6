package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

/** Four approved lecture-improvement operations using the existing session and envelope. */
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
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        CurrentUser user = user(request, false);
        return ApiResponse.ok(service.list(new LectureImprovementSearchCriteria(
                page, pageSize, (long) page * pageSize, normalized(managementItemCode),
                normalized(managementNo), normalized(teacherName), normalized(achievementStatus)), user), id(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<LectureImprovementRow> getLectureImprovement(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, false)), id(request));
    }

    @PostMapping
    public ApiResponse<LectureImprovementSaveResult> createLectureImprovement(
            @Valid @RequestBody LectureImprovementRequest body, HttpServletRequest request) {
        CurrentUser user = user(request, true);
        String requestId = id(request);
        return ApiResponse.ok(service.create(body, user, requestId), requestId);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<LectureImprovementSaveResult> updateLectureImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureImprovementRequest body, HttpServletRequest request) {
        CurrentUser user = user(request, true);
        String requestId = id(request);
        return ApiResponse.ok(service.update(achievementId, body, user, requestId), requestId);
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        CurrentUser user = request.getAttribute("currentUser") instanceof CurrentUser current ? current : null;
        LectureImprovementService.requireRole(user, write);
        return user;
    }

    private String id(HttpServletRequest request) {
        if (request.getAttribute("requestId") instanceof String value) {
            return value;
        }
        String header = request.getHeader("X-Request-Id");
        String value = header == null || header.isBlank() ? UUID.randomUUID().toString() : header.trim();
        request.setAttribute("requestId", value);
        return value;
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
