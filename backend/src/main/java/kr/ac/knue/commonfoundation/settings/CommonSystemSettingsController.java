package kr.ac.knue.commonfoundation.settings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommonSystemSettingsController {
    private final CommonSystemSettingsService service;

    public CommonSystemSettingsController(CommonSystemSettingsService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/system-settings/common")
    public ApiResponse<CommonSystemSettingsResponse> getCommonSystemSettings() {
        return ApiResponse.ok(service.getCommonSystemSettings());
    }

    @PutMapping("/api/admin/system-settings/common")
    public ApiResponse<CommonSystemSettingsResponse> saveCommonSystemSettings(@Valid @RequestBody CommonSystemSettingsRequest request,
                                                                              HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveCommonSystemSettings(request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
