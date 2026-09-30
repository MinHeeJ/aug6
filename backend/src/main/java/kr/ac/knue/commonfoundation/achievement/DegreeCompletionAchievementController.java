package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the R01/R02/R04 degree-completion achievement list and atomic save API contract. */
@RestController
public class DegreeCompletionAchievementController {
    private final DegreeCompletionAchievementService service;

    public DegreeCompletionAchievementController(DegreeCompletionAchievementService service) {
        this.service = service;
    }

    @GetMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionAchievementResponse.Search> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) EducationAchievementStatus certificationStatus,
            HttpServletRequest request) {
        validatePage(page, pageSize);
        return ApiResponse.ok(service.list(new DegreeCompletionAchievementSearchCriteria(page, pageSize, managementNo,
                teacherName, certificationStatus), readUser(request)));
    }

    @PostMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionAchievementResponse.Row> save(
            @Valid @RequestBody DegreeCompletionAchievementRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.save(body, user(request), request.getHeader("X-Request-Id")),
                request.getHeader("X-Request-Id"));
    }

    private CurrentUser user(HttpServletRequest request) {
        CurrentUser user = authenticatedUser(request);
        if (user.roles() == null || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser readUser(HttpServletRequest request) {
        CurrentUser user = authenticatedUser(request);
        if (user.roles() == null || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04", "R09").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser authenticatedUser(HttpServletRequest request) {
        Object value = request.getAttribute("currentUser");
        if (!(value instanceof CurrentUser user)) throw new UnauthenticatedException();
        return user;
    }

    private void validatePage(int page, int pageSize) {
        if (page < 0 || !List.of(20, 50, 100).contains(pageSize)) {
            throw new BusinessValidationException("검색조건이 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "pageSize는 20, 50, 100 중 하나여야 합니다.")));
        }
    }
}
