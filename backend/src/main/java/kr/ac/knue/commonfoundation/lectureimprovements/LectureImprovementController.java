package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.web.bind.annotation.*;

/** Transports the four approved lecture-improvement operations through the existing session envelope. */
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
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(page, pageSize, managementItemCode, achievementStatus,
                principal(request, false)), trace(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<LectureImprovementRow> getLectureImprovement(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, principal(request, false)), trace(request));
    }

    @PostMapping
    public ApiResponse<LectureImprovementSaveResult> createLectureImprovement(
            @Valid @RequestBody LectureImprovementRequest body, HttpServletRequest request) {
        String requestId = trace(request);
        return ApiResponse.ok(service.create(body, principal(request, true), requestId), requestId);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<LectureImprovementSaveResult> updateLectureImprovement(
            @PathVariable Long achievementId, @Valid @RequestBody LectureImprovementRequest body,
            HttpServletRequest request) {
        String requestId = trace(request);
        return ApiResponse.ok(service.update(achievementId, body, principal(request, true), requestId), requestId);
    }

    private CurrentUser principal(HttpServletRequest request, boolean write) {
        if (!(request.getAttribute("currentUser") instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        Set<String> roles = write ? Set.of("R01", "R09") : Set.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(roles::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private String trace(HttpServletRequest request) {
        String id = request.getHeader("X-Request-Id");
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id.substring(0, Math.min(id.length(), 100));
    }
}
