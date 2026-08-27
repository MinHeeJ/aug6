package kr.ac.knue.commonfoundation.securitysessions;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActiveSessionController {
    private final ActiveSessionService service;

    public ActiveSessionController(ActiveSessionService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/security/active-sessions")
    public ApiResponse<ActiveSessionSearchResponse> listActiveSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String filter,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.listActiveSessions(page, effectiveSize(size, pageSize), new ActiveSessionSearchCriteria(filter, "ACTIVE")), requestId);
    }

    @GetMapping("/api/admin/security/session-termination-histories")
    public ApiResponse<SessionTerminationHistorySearchResponse> listSessionTerminationHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String terminationType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.listSessionTerminationHistories(page, effectiveSize(size, pageSize),
                new SessionTerminationHistorySearchCriteria(filter, terminationType, fromDate, toDate)), requestId);
    }

    @PostMapping("/api/admin/security/active-sessions/{sessionId}/terminate")
    public ApiResponse<ActiveSessionRow> terminateActiveSession(@PathVariable String sessionId,
            @RequestBody(required = false) TerminateActiveSessionRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        validateRequestBeforeService(sessionId, request);
        String effectiveRequestId = requestId == null || requestId.trim().isBlank()
                ? java.util.UUID.randomUUID().toString()
                : requestId.trim();
        ActiveSessionRow terminated = service.terminateActiveSession(sessionId, request, user.userId(), effectiveRequestId);
        return ApiResponse.ok(terminated, effectiveRequestId);
    }

    private void validateRequestBeforeService(String sessionId, TerminateActiveSessionRequest request) {
        if (sessionId == null || sessionId.trim().isBlank() || request == null || request.getReason() == null || request.getReason().trim().isBlank()) {
            throw new BusinessValidationException("세션 강제종료 요청이 올바르지 않습니다.",
                    java.util.List.of(new ValidationError(request == null ? "body" : "reason", request == null ? "강제종료 요청 본문이 필요합니다." : "강제종료 사유를 입력하세요.")));
        }
    }

    private int effectiveSize(int size, Integer pageSize) {
        return pageSize == null ? size : pageSize;
    }

    private CurrentUser requireR09(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R09")) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }
}
