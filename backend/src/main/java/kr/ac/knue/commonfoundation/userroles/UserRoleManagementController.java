package kr.ac.knue.commonfoundation.userroles;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserRoleManagementController {
    private final UserRoleManagementService service;

    public UserRoleManagementController(UserRoleManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/user-roles")
    public ApiResponse<UserRoleAssignmentSearchResponse> listUserRoleAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String roleCodeFilter,
            @RequestParam(required = false) String filter) {
        return ApiResponse.ok(service.listAssignments(new UserRoleAssignmentSearchCriteria(page, size, roleCodeFilter, filter)));
    }

    @GetMapping("/api/admin/users/{userId}/roles")
    public ApiResponse<UserRoleAssignmentSearchResponse> listCurrentUserRoles(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listCurrentUserRoles(userId, page, size));
    }

    @PostMapping("/api/admin/user-roles")
    public ApiResponse<UserRoleAssignmentSummary> assignUserRole(@Valid @RequestBody UserRoleAssignmentRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.assign(request, currentUser(servletRequest).userId()));
    }

    @PutMapping("/api/admin/user-roles/{assignmentId}")
    public ApiResponse<UserRoleAssignmentSummary> updateUserRole(@PathVariable Long assignmentId, @Valid @RequestBody UserRoleAssignmentRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.update(assignmentId, request, currentUser(servletRequest).userId()));
    }

    @DeleteMapping("/api/admin/user-roles/{assignmentId}")
    public ApiResponse<UserRoleAssignmentSummary> revokeUserRole(@PathVariable Long assignmentId, @Valid @RequestBody RevokeUserRoleRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.revoke(assignmentId, request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
