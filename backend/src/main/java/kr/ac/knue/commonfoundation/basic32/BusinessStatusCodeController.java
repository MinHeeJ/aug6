package kr.ac.knue.commonfoundation.basic32;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BusinessStatusCodeController {
    private final BusinessStatusCodeService service;

    public BusinessStatusCodeController(BusinessStatusCodeService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/business-status-codes")
    public ApiResponse<BusinessStatusCodeSearchResponse> listBusinessStatusCodes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String definitionVersion,
            @RequestParam(required = false) String statusCode,
            HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        return ApiResponse.ok(service.list(new BusinessStatusCodeSearchCriteria(page, size, businessType, definitionVersion, statusCode)));
    }

    @PostMapping("/api/admin/business-status-codes")
    public ApiResponse<BusinessStatusCodeRow> saveBusinessStatusCode(
            @Valid @RequestBody BusinessStatusCodeSaveRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireAdmin(servletRequest);
        return ApiResponse.ok(service.save(request, user.userId()));
    }

    private CurrentUser requireAdmin(HttpServletRequest request) {
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
