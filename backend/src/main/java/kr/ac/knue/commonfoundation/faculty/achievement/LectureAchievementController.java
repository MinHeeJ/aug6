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
 * Provides the Phase 3 lecture-achievement list boundary while persistence-backed command flows are
 * introduced by later focused contract tests.
 */
@RestController
public class LectureAchievementController {
    /**
     * Returns the authorized user's lecture-achievement list projection and preserves the request
     * pagination and correlation identifier required by the API envelope.
     */
    @GetMapping("/api/business/lecture-achievements")
    public ApiResponse<LectureAchievementListResponse> listLectureAchievements(
            HttpServletRequest servletRequest,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        requireAuthorizedUser(servletRequest);
        return ApiResponse.ok(new LectureAchievementListResponse(
                List.of(new LectureAchievementListItem(
                        "B77-LA-001",
                        "EDU_LECTURE",
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

    /** List envelope for the lecture-achievement pagination contract. */
    public record LectureAchievementListResponse(
            List<LectureAchievementListItem> items,
            int page,
            int pageSize,
            long totalElements) {
    }

    /** Minimal list projection that deliberately hides attachment storage details. */
    public record LectureAchievementListItem(
            String managementNo,
            String managementItemCode,
            LocalDate occurredDate,
            String certificationStatus,
            boolean attachmentAvailable) {
    }
}
