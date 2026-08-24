package kr.ac.knue.commonfoundation.fileoperations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FilePolicyManagementController {
    private final FilePolicyManagementService service;

    public FilePolicyManagementController(FilePolicyManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/file-policies")
    public ApiResponse<FilePolicySearchResponse> listFilePolicies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(name = "filter", required = false) String filter,
            HttpServletRequest servletRequest) {
        requireR09(currentUser(servletRequest));
        return ApiResponse.ok(service.listFilePolicies(page, size, filter));
    }

    @PutMapping("/api/admin/file-policies-save")
    public ApiResponse<FilePolicyRow> saveFilePolicy(
            @Valid @RequestBody FilePolicySaveRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = currentUser(servletRequest);
        requireR09(user);
        return ApiResponse.ok(service.saveFilePolicy(request, user.userId()));
    }

    private void requireR09(CurrentUser user) {
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
}
