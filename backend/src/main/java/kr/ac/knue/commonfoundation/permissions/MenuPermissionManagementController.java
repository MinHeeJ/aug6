package kr.ac.knue.commonfoundation.permissions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuPermissionManagementController {
    private final MenuPermissionManagementService service;

    public MenuPermissionManagementController(MenuPermissionManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/menu-permissions")
    public ApiResponse<MenuPermissionSearchResponse> listMenuPermissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String filter) {
        return ApiResponse.ok(service.listMenuPermissions(new MenuPermissionSearchCriteria(page, size, targetType, targetId, filter)));
    }

    @PutMapping("/api/admin/menu-permissions")
    public ApiResponse<MenuPermissionRow> saveMenuPermissions(@Valid @RequestBody MenuPermissionSaveRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveMenuPermission(request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
