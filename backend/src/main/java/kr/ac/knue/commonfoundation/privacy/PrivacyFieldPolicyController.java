package kr.ac.knue.commonfoundation.privacy;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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
public class PrivacyFieldPolicyController {
    private final PrivacyFieldPolicyService service;

    public PrivacyFieldPolicyController(PrivacyFieldPolicyService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/privacy/policies")
    public ApiResponse<PrivacyFieldPolicySearchResponse> listPrivacyFieldPolicies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String fieldKey,
            @RequestParam(required = false) String privacyGrade,
            @RequestParam(required = false) String encryptionRequiredYn,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.listPrivacyFieldPolicies(new PrivacyFieldPolicySearchCriteria(page, size, fieldKey, privacyGrade, encryptionRequiredYn)));
    }

    @PutMapping("/api/admin/privacy/policies-save")
    public ApiResponse<List<PrivacyFieldPolicyRow>> savePrivacyFieldPolicies(
            @RequestBody List<PrivacyFieldPolicySaveRequest> requests,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        return ApiResponse.ok(service.savePrivacyFieldPolicies(requests, user.userId()));
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
