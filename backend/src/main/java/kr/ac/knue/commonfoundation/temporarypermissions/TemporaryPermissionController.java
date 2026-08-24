package kr.ac.knue.commonfoundation.temporarypermissions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TemporaryPermissionController {
    private final TemporaryPermissionService service;

    public TemporaryPermissionController(TemporaryPermissionService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/temporary-permissions")
    public ApiResponse<TemporaryPermissionSearchResponse> listTemporaryPermissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId) {
        return ApiResponse.ok(service.listTemporaryPermissions(new TemporaryPermissionSearchCriteria(page, size, userId)));
    }

    @PostMapping("/api/admin/temporary-permissions-create")
    public ApiResponse<TemporaryPermissionRow> createTemporaryPermission(@Valid @RequestBody TemporaryPermissionCreateRequest request,
                                                                         HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.createTemporaryPermission(request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
