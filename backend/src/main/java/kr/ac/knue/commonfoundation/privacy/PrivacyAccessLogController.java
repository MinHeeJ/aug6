package kr.ac.knue.commonfoundation.privacy;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrivacyAccessLogController {
    private final PrivacyAccessLogService service;

    public PrivacyAccessLogController(PrivacyAccessLogService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/privacy/access-logs")
    public ApiResponse<PrivacyAccessLogSearchResponse> searchPrivacyAccessLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String targetRef,
            @RequestParam(required = false) String processType,
            @RequestParam(required = false) String processedFrom,
            @RequestParam(required = false) String processedTo,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.searchPrivacyAccessLogs(
                new PrivacyAccessLogSearchCriteria(page, size, actorUserId, targetRef, processType, processedFrom, processedTo)));
    }

    @GetMapping("/api/admin/privacy/access-logs/{historyId}")
    public ApiResponse<PrivacyAccessLogRow> getPrivacyAccessLog(
            @PathVariable Long historyId,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.getPrivacyAccessLog(historyId));
    }

    @PostMapping("/api/admin/privacy/access-logs-record")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PrivacyAccessLogRow> recordPrivacyAccessLog(
            @RequestBody PrivacyAccessLogRecordRequest request,
            HttpServletRequest servletRequest) {
        currentUser(servletRequest);
        return ApiResponse.ok(service.recordPrivacyAccessLog(request, resolveRequestIp(servletRequest, request)));
    }

    private void requireR09(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (!user.roles().contains("R09")) {
            throw new ForbiddenException();
        }
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }

    private String resolveRequestIp(HttpServletRequest request, PrivacyAccessLogRecordRequest body) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (body != null && body.requestIp() != null && !body.requestIp().isBlank()) {
            return body.requestIp().trim();
        }
        return request.getRemoteAddr();
    }
}
