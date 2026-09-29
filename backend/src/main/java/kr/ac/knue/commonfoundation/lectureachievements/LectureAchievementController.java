package kr.ac.knue.commonfoundation.lectureachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.RequestIdentifierFilter;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the OpenAPI contract for searching and saving education-area lecture achievements.
 * The controller only transports the authenticated principal; authorization and persistence remain in the service.
 */
@RestController
public class LectureAchievementController {
    private final LectureAchievementService service;

    public LectureAchievementController(LectureAchievementService service) { this.service = service; }

    /** Lists permitted lecture achievements using the contract's optional filters and page sizes. */
    @GetMapping("/api/business/lecture-achievements")
    public ApiResponse<LectureAchievementSearchResponse> listLectureAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String certificationStatus,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.list(new LectureAchievementSearchCriteria(managementNo, teacherName,
                managementItemCode, certificationStatus), page, pageSize, currentUser(servletRequest)),
                RequestIdentifierFilter.requestId(servletRequest));
    }

    /** Saves a lecture achievement with its attachment reference and any permitted status transition. */
    @PostMapping("/api/business/lecture-achievements")
    public ApiResponse<LectureAchievementRow> saveLectureAchievement(
            @Valid @RequestBody LectureAchievementSaveRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.save(request, currentUser(servletRequest)), RequestIdentifierFilter.requestId(servletRequest));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) return currentUser;
        throw new UnauthenticatedException();
    }
}
