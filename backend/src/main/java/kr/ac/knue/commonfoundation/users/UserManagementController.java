package kr.ac.knue.commonfoundation.users;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserManagementController {
    private final UserManagementService service;

    public UserManagementController(UserManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/users")
    public ApiResponse<UserSearchResponse> searchUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String employeeNo,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String organizationCodeFilter,
            @RequestParam(required = false) String rankName,
            @RequestParam(required = false) String employmentStatus,
            @RequestParam(required = false) String roleCodeFilter,
            @RequestParam(required = false) String systemUseYn,
            @RequestParam(required = false) String filter) {
        return ApiResponse.ok(service.search(new UserSearchCriteria(page, size, employeeNo, name, organizationCodeFilter, rankName, employmentStatus, roleCodeFilter, systemUseYn, filter)));
    }

    @PatchMapping("/api/admin/users/{userId}/account")
    public ApiResponse<UserSummary> updateUserAccount(@PathVariable Long userId, @Valid @RequestBody UpdateUserAccountRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.updateAccount(userId, request, currentUser(servletRequest).userId()));
    }

    @PatchMapping("/api/admin/users/{userId}/roles")
    public ApiResponse<List<UserRoleSummary>> updateUserRoles(@PathVariable Long userId, @Valid @RequestBody UpdateUserRolesRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.updateRoles(userId, request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
