package kr.ac.knue.commonfoundation.functionpermissions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FunctionPermissionController {
    private final FunctionPermissionService service;

    public FunctionPermissionController(FunctionPermissionService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/function-permissions")
    public ApiResponse<FunctionPermissionSearchResponse> listFunctionPermissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String screenId,
            @RequestParam(required = false) String roleCode) {
        return ApiResponse.ok(service.listFunctionPermissions(new FunctionPermissionSearchCriteria(page, size, screenId, roleCode)));
    }

    @PutMapping("/api/admin/function-permissions-save")
    public ApiResponse<FunctionPermissionRow> saveFunctionPermissions(@Valid @RequestBody FunctionPermissionSaveRequest request,
                                                                      HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveFunctionPermission(request, currentUser(servletRequest).userId()));
    }

    @PostMapping("/api/admin/function-permissions/evaluate")
    public ApiResponse<FunctionPermissionEvaluateResponse> evaluateFunctionPermission(@Valid @RequestBody FunctionPermissionEvaluateRequest request) {
        return ApiResponse.ok(service.evaluate(request));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
