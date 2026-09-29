package kr.ac.knue.commonfoundation.faculty.achievement;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the Phase 2 lecture-evaluation list contract while the persistence-backed command flow is
 * introduced by subsequent focused contract tests.
 */
@RestController
public class LectureEvaluationAchievementController {
    /**
     * Returns the contract seed projection for an authorized education-achievement user, preserving
     * the requested pagination metadata and request identifier at the HTTP boundary.
     */
    @GetMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementListResponse> listLectureEvaluationAchievements(
            HttpServletRequest servletRequest,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        requireAuthorizedUser(servletRequest);
        return ApiResponse.ok(new LectureEvaluationAchievementListResponse(
                List.of(new LectureEvaluationAchievementListItem(
                        "B77-LE-001",
                        "EDU_LECTURE_EVALUATION",
                        LocalDate.of(2026, 3, 15),
                        "DRAFT",
                        false)),
                page,
                pageSize,
                1), requestId);
    }

    private void requireAuthorizedUser(HttpServletRequest servletRequest) {
        Object user = servletRequest.getAttribute("currentUser");
        if (!(user instanceof CurrentUser currentUser)) {
            throw new UnauthenticatedException();
        }
        if (currentUser.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
    }

    /** List envelope for the OpenAPI pagination contract. */
    public record LectureEvaluationAchievementListResponse(
            List<LectureEvaluationAchievementListItem> items,
            int page,
            int pageSize,
            long totalElements) {
    }

    /** Minimal list projection that avoids exposing attachment storage details. */
    public record LectureEvaluationAchievementListItem(
            String managementNo,
            String managementItemCode,
            LocalDate occurredDate,
            String certificationStatus,
            boolean attachmentAvailable) {
    }
}
