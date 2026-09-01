package kr.ac.knue.commonfoundation.basic36;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AchievementDataHistoryController {
    private final AchievementDataHistoryService service;

    public AchievementDataHistoryController(AchievementDataHistoryService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/achievement-data-histories")
    public ApiResponse<AchievementDataHistorySearchResponse> listAchievementDataHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String achievementType,
            @RequestParam(required = false) String achievementKey,
            @RequestParam(required = false) String employeeNo,
            @RequestParam(required = false) String changedAtFrom,
            @RequestParam(required = false) String changedAtTo,
            @RequestParam(required = false) String changeType,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser currentUser = requireUser(servletRequest);
        return ApiResponse.ok(service.listHistories(new AchievementDataHistorySearchCriteria(page, size, achievementType, achievementKey, employeeNo, changedAtFrom, changedAtTo, changeType), currentUser), effectiveRequestId(requestId));
    }

    @GetMapping("/api/admin/achievement-data-as-of")
    public ApiResponse<AchievementDataAsOfSearchResponse> listAchievementDataAsOf(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String achievementType,
            @RequestParam(required = false) String achievementKey,
            @RequestParam(required = false) String employeeNo,
            @RequestParam(required = false) String asOfAt,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser currentUser = requireUser(servletRequest);
        return ApiResponse.ok(service.listAsOf(new AchievementDataAsOfSearchCriteria(page, size, achievementType, achievementKey, employeeNo, asOfAt), currentUser), effectiveRequestId(requestId));
    }

    private CurrentUser requireUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R04") || currentUser.roles().contains("R08") || currentUser.roles().contains("R09")) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) return requestId.trim();
        return UUID.randomUUID().toString();
    }
}
