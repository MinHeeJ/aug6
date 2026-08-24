package kr.ac.knue.commonfoundation.permissionops;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PermissionChangeHistoryController {
    private final PermissionChangeHistoryService service;

    public PermissionChangeHistoryController(PermissionChangeHistoryService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/permission-history")
    public ApiResponse<PermissionChangeHistorySearchResponse> listPermissionChangeHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            HttpServletRequest servletRequest) {
        currentUser(servletRequest);
        return ApiResponse.ok(service.listPermissionChangeHistory(new PermissionChangeHistorySearchCriteria(
                page,
                size,
                targetType,
                targetId)));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
