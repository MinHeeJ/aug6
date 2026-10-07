package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.web.bind.annotation.*;

/** Approved list/detail/create/update HTTP boundary for lecture improvements. */
@RestController
@RequestMapping("/api/business/lecture-improvements")
public class LectureImprovementController {
    private final LectureImprovementService service;

    public LectureImprovementController(LectureImprovementService service) {
        this.service = service;
    }

    /** Lists only rows visible to the authenticated principal. */
    @GetMapping
    public ApiResponse<LectureImprovementSearchResponse> listLectureImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester,
            HttpServletRequest request) {
        String requestId = RequestIds.resolve(request);
        if (page < 0 || (pageSize != 20 && pageSize != 50 && pageSize != 100)) {
            throw new IllegalArgumentException("페이지와 표시 건수를 확인하세요.");
        }
        return ApiResponse.ok(service.list(new LectureImprovementSearch(page, pageSize, (long) page * pageSize,
                managementItemCode == null || managementItemCode.isBlank() ? null : managementItemCode.trim(),
                academicYear, semester), user(request)), requestId);
    }

    /** Returns the same typed row as the save result. */
    @GetMapping("/{achievementId}")
    public ApiResponse<LectureImprovementRow> getLectureImprovement(@PathVariable Long achievementId,
            HttpServletRequest request) {
        String requestId = RequestIds.resolve(request);
        return ApiResponse.ok(service.get(achievementId, user(request)), requestId);
    }

    /** Creates a new identity, never an implicit upsert. */
    @PostMapping
    public ApiResponse<LectureImprovementSaveResult> createLectureImprovement(
            @Valid @RequestBody LectureImprovementRequest body, HttpServletRequest request) {
        String requestId = RequestIds.resolve(request);
        return ApiResponse.ok(service.create(body, user(request), requestId), requestId);
    }

    /** Uses only the selected path identity for update. */
    @PutMapping("/{achievementId}")
    public ApiResponse<LectureImprovementSaveResult> updateLectureImprovement(@PathVariable Long achievementId,
            @Valid @RequestBody LectureImprovementRequest body, HttpServletRequest request) {
        String requestId = RequestIds.resolve(request);
        return ApiResponse.ok(service.update(achievementId, body, user(request), requestId), requestId);
    }

    private CurrentUser user(HttpServletRequest request) {
        if (!(request.getAttribute("currentUser") instanceof CurrentUser user)) throw new UnauthenticatedException();
        return user;
    }
}
