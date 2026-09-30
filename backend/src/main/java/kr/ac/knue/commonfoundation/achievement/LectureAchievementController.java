package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owns the lecture achievement HTTP boundary and keeps session authorization outside business persistence.
 */
@RestController
public class LectureAchievementController {
    private final LectureAchievementService service;
    private final LectureAchievementFoundationService foundationService;

    public LectureAchievementController(LectureAchievementService service,
                                                  LectureAchievementFoundationService foundationService) {
        this.service = service;
        this.foundationService = foundationService;
    }

    /** Implements listLectureAchievements using the required relative /api route. */
    @GetMapping("/api/business/lecture-achievements")
    public ApiResponse<LectureAchievementDtos.SearchResponse> listLectureAchievements(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String organizationCode,
            @RequestParam(required = false) String certificationStatus, @RequestParam(required = false) String managementItemCode,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        validateSize(size);
        CurrentUser user = requireReadRole(servletRequest);
        return ApiResponse.ok(service.list(new LectureAchievementDtos.SearchCriteria(page, size, evaluationYear,
                organizationCode, certificationStatus, managementItemCode), user), requestId(requestId));
    }

    /** Saves a draft or existing lecture achievement after server-side validation. */
    @PostMapping("/api/business/lecture-achievements")
    public ApiResponse<LectureAchievementDtos.SaveResponse> saveLectureAchievement(
            @Valid @RequestBody LectureAchievementDtos.SaveRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        CurrentUser user = requireRole(servletRequest, "R01", "R02", "R04");
        return ApiResponse.ok(service.save(body, user), requestId(requestId));
    }

    /** Records an allowed certification-status transition and its immutable audit rows. */
    @PostMapping("/api/business/lecture-achievements/{achievementId}/status")
    public ApiResponse<Void> transitionStatus(@PathVariable Long achievementId,
            @Valid @RequestBody EducationAchievementTransitionRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        foundationService.transitionLecture(achievementId, body, requireRole(servletRequest, "R01", "R02", "R04"));
        return ApiResponse.ok(null, requestId(requestId));
    }

    /** Updates only an opaque attachment reference; actual binary storage remains behind the shared attachment service. */
    @PutMapping("/api/business/lecture-achievements/{achievementId}/attachment-reference")
    public ApiResponse<LectureAchievementDtos.Row> saveAttachmentReference(@PathVariable Long achievementId,
            @Valid @RequestBody LectureAchievementDtos.AttachmentReferenceRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveAttachmentReference(achievementId, body, requireRole(servletRequest, "R01", "R02", "R04")), requestId(requestId));
    }

    private CurrentUser requireReadRole(HttpServletRequest request) {
        return requireRole(request, "R01", "R02", "R04", "R09");
    }

    private CurrentUser requireRole(HttpServletRequest request, String... roles) {
        Object currentUser = request.getAttribute("currentUser");
        if (!(currentUser instanceof CurrentUser user)) throw new UnauthenticatedException();
        for (String role : roles) if (user.roles().contains(role)) return user;
        throw new ForbiddenException();
    }

    private void validateSize(int size) {
        if (size != 20 && size != 50 && size != 100) {
            throw new BusinessValidationException("목록 표시 건수가 올바르지 않습니다.", List.of(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String requestId(String value) { return value != null && !value.isBlank() ? value.trim() : UUID.randomUUID().toString(); }
}
