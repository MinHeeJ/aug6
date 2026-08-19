package kr.ac.knue.commonfoundation.menus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuStructureManagementController {
    private final MenuStructureManagementService service;

    public MenuStructureManagementController(MenuStructureManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/menus/tree")
    public ApiResponse<List<MenuTreeNode>> getMenuTree(@RequestParam(required = false) String filter) {
        return ApiResponse.ok(service.getMenuTree(filter));
    }

    @PutMapping("/api/admin/menus/{menuId}/parent")
    public ApiResponse<MenuTreeNode> updateMenuParent(
            @PathVariable Long menuId,
            @Valid @RequestBody MenuParentUpdateRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.updateMenuParent(menuId, request, currentUser(servletRequest).userId()));
    }

    @PutMapping("/api/admin/menus/reorder")
    public ApiResponse<List<MenuTreeNode>> reorderMenus(@Valid @RequestBody MenuReorderRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.reorderMenus(request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
