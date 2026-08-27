package kr.ac.knue.commonfoundation.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SensitiveInformationAccessLogController {
    private final SensitiveInformationAccessLogService service;

    public SensitiveInformationAccessLogController(SensitiveInformationAccessLogService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/audit/sensitive-information-access-logs")
    public ApiResponse<SensitiveInformationAccessLogSearchResponse> listSensitiveInformationAccessLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String informationType,
            @RequestParam(required = false) Long viewerUserId,
            @RequestParam(required = false) String accessResult,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.listSensitiveInformationAccessLogs(page, pageSize == null ? size : pageSize,
                new SensitiveInformationAccessLogSearchCriteria(filter, informationType, viewerUserId, accessResult, fromDate, toDate)), requestId);
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
