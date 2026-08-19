package kr.ac.knue.commonfoundation.menus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuExecutionController {
    private final MenuExecutionService service;

    public MenuExecutionController(MenuExecutionService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/menus/{menuId}/execution")
    public ApiResponse<MenuExecutionRow> getMenuExecution(@PathVariable Long menuId) {
        return ApiResponse.ok(service.getMenuExecution(menuId));
    }

    @PutMapping("/api/admin/menus/{menuId}/execution")
    public ApiResponse<MenuExecutionRow> updateMenuExecution(@PathVariable Long menuId,
                                                             @Valid @RequestBody MenuExecutionRequest request,
                                                             HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.updateMenuExecution(menuId, request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
