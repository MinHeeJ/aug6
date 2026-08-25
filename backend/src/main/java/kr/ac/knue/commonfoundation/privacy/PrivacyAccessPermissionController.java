package kr.ac.knue.commonfoundation.privacy;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrivacyAccessPermissionController {
    private final PrivacyAccessPermissionService service;

    public PrivacyAccessPermissionController(PrivacyAccessPermissionService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/privacy/permissions")
    public ApiResponse<PrivacyAccessPermissionSearchResponse> listPrivacyAccessPermissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String fieldKey,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.listPrivacyAccessPermissions(new PrivacyAccessPermissionSearchCriteria(page, size, roleCode, fieldKey)));
    }

    @PutMapping("/api/admin/privacy/permissions-save")
    public ApiResponse<List<PrivacyAccessPermissionRow>> savePrivacyAccessPermissions(
            @RequestBody List<PrivacyAccessPermissionSaveRequest> requests,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        return ApiResponse.ok(service.savePrivacyAccessPermissions(requests, user.userId()));
    }

    @PostMapping("/api/admin/privacy/permissions/evaluate")
    public ApiResponse<PrivacyAccessEvaluateResponse> evaluatePrivacyAccessPermission(
            @RequestBody PrivacyAccessEvaluateRequest request,
            HttpServletRequest servletRequest) {
        currentUser(servletRequest);
        return ApiResponse.ok(service.evaluatePrivacyAccessPermission(request));
    }

    private CurrentUser requireR09(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (!user.roles().contains("R09")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
