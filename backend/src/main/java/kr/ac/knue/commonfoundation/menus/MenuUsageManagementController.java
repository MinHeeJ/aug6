package kr.ac.knue.commonfoundation.menus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuUsageManagementController {
    private final MenuUsageManagementService service;

    public MenuUsageManagementController(MenuUsageManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/menus/usage-settings")
    public ApiResponse<MenuUsageSearchResponse> listMenuUsageSettings(@RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       @RequestParam(required = false) String filter,
                                                                       @RequestParam(required = false) String systemUseYn) {
        return ApiResponse.ok(service.listMenuUsageSettings(new MenuUsageSearchCriteria(page, size, filter, systemUseYn)));
    }

    @PutMapping("/api/admin/menus/usage-settings")
    public ApiResponse<List<MenuUsageRow>> saveMenuUsageSettings(@Valid @RequestBody MenuUsageSettingsRequest request,
                                                                 HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveMenuUsageSettings(request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
