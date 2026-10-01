package com.example.faculty.achievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the role-protected lecture-evaluation achievement list and save operations required by BASIC-79.
 */
@RestController
public class LectureEvaluationAchievementController {
    private final LectureEvaluationAchievementService service;

    public LectureEvaluationAchievementController(LectureEvaluationAchievementService service) {
        this.service = service;
    }

    /**
     * Lists the requester's visible lecture-evaluation achievements using dynamically bound search filters.
     */
    @GetMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementSearchResponse> listLectureEvaluationAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) LocalDate occurredDateFrom,
            @RequestParam(required = false) LocalDate occurredDateTo,
            @RequestParam(required = false) String certificationStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest
    ) {
        CurrentUser user = requireAchievementUser(servletRequest);
        return ApiResponse.ok(
                service.list(
                        new LectureEvaluationAchievementSearchCriteria(
                                page,
                                pageSize,
                                managementNo,
                                teacherName,
                                managementItemCode,
                                occurredDateFrom,
                                occurredDateTo,
                                certificationStatus,
                                null,
                                false
                        ),
                        user
                ),
                effectiveRequestId(requestId)
        );
    }

    /**
     * Saves a lecture-evaluation source row and optional state transition after server-side rule checks.
     */
    @PostMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementSaveResult> saveLectureEvaluationAchievement(
            @Valid @RequestBody SaveLectureEvaluationAchievementRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest
    ) {
        CurrentUser user = requireAchievementUser(servletRequest);
        return ApiResponse.ok(service.save(request, user), effectiveRequestId(requestId));
    }

    private CurrentUser requireAchievementUser(HttpServletRequest request) {
        Object currentUser = request.getAttribute("currentUser");
        if (!(currentUser instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        if (user.roles().stream().noneMatch(role -> role.equals("R01") || role.equals("R02") || role.equals("R04"))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
