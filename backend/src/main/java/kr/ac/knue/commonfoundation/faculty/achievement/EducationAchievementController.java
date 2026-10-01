package kr.ac.knue.commonfoundation.faculty.achievement;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP boundary for the R01/R02/R04 lecture-evaluation achievement list and creation contract.
 */
@RestController
public class EducationAchievementController {
    private final EducationAchievementService service;

    public EducationAchievementController(EducationAchievementService service) {
        this.service = service;
    }

    /** Returns the paginated list required by listLectureEvaluationAchievements. */
    @GetMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementSearchResponse> listLectureEvaluationAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurredDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurredDateTo,
            @RequestParam(required = false) String certificationStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest
    ) {
        String effectiveRequestId = effectiveRequestId(requestId);
        CurrentUser currentUser = requireAchievementRole(currentUser(servletRequest));
        return ApiResponse.ok(
                service.listLectureEvaluationAchievements(
                        new LectureEvaluationAchievementSearchCriteria(
                                page,
                                size,
                                managementNo,
                                teacherName,
                                managementItemCode,
                                occurredDateFrom,
                                occurredDateTo,
                                certificationStatus
                        ),
                        currentUser
                ),
                effectiveRequestId
        );
    }

    /** Creates one DRAFTING source record through saveLectureEvaluationAchievement. */
    @PostMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<SaveLectureEvaluationAchievementResult> saveLectureEvaluationAchievement(
            @RequestBody SaveLectureEvaluationAchievementRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest
    ) {
        String effectiveRequestId = effectiveRequestId(requestId);
        CurrentUser currentUser = requireAchievementRole(currentUser(servletRequest));
        return ApiResponse.ok(
                service.saveLectureEvaluationAchievement(request, currentUser, effectiveRequestId),
                effectiveRequestId
        );
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }

    private CurrentUser requireAchievementRole(CurrentUser currentUser) {
        if (currentUser.roles().stream().anyMatch(Set.of("R01", "R02", "R04", "R09")::contains)) {
            return currentUser;
        }
        throw new ForbiddenException();
    }

    private String effectiveRequestId(String requestId) {
        return requestId != null && !requestId.trim().isBlank()
                ? requestId.trim()
                : UUID.randomUUID().toString();
    }
}
