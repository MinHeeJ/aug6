package kr.ac.knue.commonfoundation.codes;

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
public class DetailCodeUsageManagementController {
    private final DetailCodeUsageManagementService service;

    public DetailCodeUsageManagementController(DetailCodeUsageManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/code-groups/{groupId}/codes/usage-settings")
    public ApiResponse<DetailCodeUsageSearchResponse> listDetailCodeUsageSettings(@PathVariable String groupId,
                                                                                   @RequestParam(defaultValue = "0") int page,
                                                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.listDetailCodeUsageSettings(groupId, page, size));
    }

    @PutMapping("/api/admin/code-groups/{groupId}/codes/usage-settings")
    public ApiResponse<List<DetailCodeUsageRow>> saveDetailCodeUsageSettings(@PathVariable String groupId,
                                                                             @Valid @RequestBody DetailCodeUsageSettingsRequest request,
                                                                             HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveDetailCodeUsageSettings(groupId, request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
