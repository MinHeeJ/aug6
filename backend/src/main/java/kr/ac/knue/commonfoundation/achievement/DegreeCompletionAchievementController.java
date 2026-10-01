package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP boundary for authorized degree-completion search and atomic header/detail saving. */
@RestController
public class DegreeCompletionAchievementController {
    private final DegreeCompletionAchievementService service;

    public DegreeCompletionAchievementController(DegreeCompletionAchievementService service) {
        this.service = service;
    }

    /** Lists degree-completion achievements with the contract's optional filters and bounded pages. */
    @GetMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionAchievementSearchResponse> listDegreeCompletionAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String certificationStatus,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(service.list(
                new DegreeCompletionAchievementSearchCriteria(
                        page,
                        size,
                        managementNo,
                        teacherName,
                        certificationStatus,
                        null,
                        null
                ),
                actor(request)
        ));
    }

    /** Saves degree-completion headers and students after service-level scope and lock validation. */
    @PostMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionAchievementRow> saveDegreeCompletionAchievement(
            @Valid @RequestBody SaveDegreeCompletionAchievementRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.save(request, actor(servletRequest)));
    }

    private CurrentUser actor(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (!(user instanceof CurrentUser currentUser)) {
            throw new UnauthenticatedException();
        }
        if (currentUser.roles().stream().noneMatch(
                role -> role.equals("R01") || role.equals("R02") || role.equals("R04") || role.equals("R09")
        )) {
            throw new ForbiddenException();
        }
        return currentUser;
    }
}
