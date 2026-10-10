package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes only the approved lecture-improvement collection and detail operations. */
@RestController
public class LectureImprovementController {
    private final LectureImprovementService service;

    public LectureImprovementController(LectureImprovementService service) {
        this.service = service;
    }

    @GetMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementSearchResponse> listLectureImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            @RequestParam(required = false) String evaluationYear,
            HttpServletRequest request) {
        CurrentUser user = user(request, false);
        return ApiResponse.ok(service.list(new LectureImprovementSearchCriteria(
                page, pageSize, 0, managementNo, teacherName, managementItemCode, achievementStatus, evaluationYear), user),
                requestId(request));
    }

    @GetMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementRow> getLectureImprovement(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.detail(achievementId, user(request, false)), requestId(request));
    }

    @PostMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementSaveResult> createLectureImprovement(
            @Valid @RequestBody LectureImprovementRequest body, HttpServletRequest request) {
        CurrentUser user = user(request, true);
        String trace = requestId(request);
        return ApiResponse.ok(service.create(body, user, trace), trace);
    }

    @PutMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementSaveResult> updateLectureImprovement(
            @PathVariable Long achievementId, @Valid @RequestBody LectureImprovementRequest body,
            HttpServletRequest request) {
        CurrentUser user = user(request, true);
        String trace = requestId(request);
        return ApiResponse.ok(service.update(achievementId, body, user, trace), trace);
    }

    private CurrentUser user(HttpServletRequest request, boolean write) {
        Object principal = request.getAttribute("currentUser");
        CurrentUser user = principal instanceof CurrentUser current ? current : null;
        requestId(request);
        LectureImprovementService.authorize(user, write);
        return user;
    }

    static String requestId(HttpServletRequest request) {
        Object existing = request.getAttribute("lectureImprovementRequestId");
        if (existing != null) {
            return existing.toString();
        }
        String header = request.getHeader("X-Request-Id");
        String trace = header == null || header.isBlank() ? UUID.randomUUID().toString() : header.trim();
        request.setAttribute("lectureImprovementRequestId", trace);
        return trace;
    }
}
