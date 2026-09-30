package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the protected lecture-evaluation achievement API required by the education-achievement workflow.
 */
@RestController
public class EducationAchievementController {
    private static final Set<String> ACHIEVEMENT_ROLES = Set.of("R01", "R02", "R04", "R09");
    private final EducationAchievementService service;

    public EducationAchievementController(EducationAchievementService service) {
        this.service = service;
    }

    /** Lists lecture-evaluation achievements using only supplied search predicates and permitted page sizes. */
    @GetMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementSearchResponse> listLectureEvaluationAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) LocalDate occurredDateFrom,
            @RequestParam(required = false) LocalDate occurredDateTo,
            @RequestParam(required = false) String certificationStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest
    ) {
        validatePageSize(size);
        CurrentUser user = requireAchievementRole(currentUser(servletRequest));
        return ApiResponse.ok(
                service.list(
                        new LectureEvaluationAchievementSearchCriteria(
                                page, size, trim(managementNo), trim(teacherName), trim(managementItemCode),
                                occurredDateFrom, occurredDateTo, trim(certificationStatus)
                        ),
                        user
                ),
                effectiveRequestId(requestId)
        );
    }

    /** Saves a lecture-evaluation achievement after service-layer scope, period, lock, and input validation. */
    @PostMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementRow> saveLectureEvaluationAchievement(
            @Valid @RequestBody LectureEvaluationAchievementRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest
    ) {
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(
                service.save(request, requireAchievementRole(currentUser(servletRequest)), effectiveRequestId),
                effectiveRequestId
        );
    }

    /** Records a permitted lecture-evaluation certification transition and its immutable history payload. */
    @PostMapping("/api/business/lecture-evaluation-achievements/{achievementId}/transitions")
    public ApiResponse<LectureEvaluationAchievementRow> transitionLectureEvaluationAchievement(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureEvaluationStatusTransitionRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest
    ) {
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(
                service.transition(
                        achievementId,
                        request,
                        requireAchievementRole(currentUser(servletRequest)),
                        effectiveRequestId
                ),
                effectiveRequestId
        );
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) return currentUser;
        throw new UnauthenticatedException();
    }

    private CurrentUser requireAchievementRole(CurrentUser user) {
        if (user.roles().stream().anyMatch(ACHIEVEMENT_ROLES::contains)) return user;
        throw new ForbiddenException();
    }

    private void validatePageSize(int size) {
        if (size != 20 && size != 50 && size != 100) {
            throw new BusinessValidationException(
                    "강의평가 목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요."))
            );
        }
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
