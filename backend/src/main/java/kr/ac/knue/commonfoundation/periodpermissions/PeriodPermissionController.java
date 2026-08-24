package kr.ac.knue.commonfoundation.periodpermissions;

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
public class PeriodPermissionController {
    private final PeriodPermissionService service;

    public PeriodPermissionController(PeriodPermissionService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/period-permissions")
    public ApiResponse<PeriodPermissionSearchResponse> listPeriodPermissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String businessPeriodId) {
        return ApiResponse.ok(service.listPeriodPermissions(new PeriodPermissionSearchCriteria(page, size, businessPeriodId)));
    }

    @PutMapping("/api/admin/period-permissions-save")
    public ApiResponse<PeriodPermissionRow> savePeriodPermissions(@Valid @RequestBody PeriodPermissionSaveRequest request,
                                                                  HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.savePeriodPermission(request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
