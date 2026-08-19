package kr.ac.knue.commonfoundation.codes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DetailCodeManagementController {
    private final DetailCodeManagementService service;

    public DetailCodeManagementController(DetailCodeManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/code-groups/{groupId}/codes")
    public ApiResponse<List<DetailCodeRow>> listDetailCodes(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String filter) {
        if (filter == null || filter.isBlank()) {
            return ApiResponse.ok(service.listDetailCodes(groupId, page, size));
        }
        return ApiResponse.ok(service.listDetailCodes(groupId, filter, page, size));
    }

    @PostMapping("/api/admin/code-groups/{groupId}/codes")
    public ApiResponse<DetailCodeRow> createDetailCode(
            @PathVariable String groupId,
            @Valid @RequestBody DetailCodeRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.createDetailCode(groupId, request, currentUser(servletRequest).userId()));
    }

    @PutMapping("/api/admin/code-groups/{groupId}/codes/{codeValue}")
    public ApiResponse<DetailCodeRow> updateDetailCode(
            @PathVariable String groupId,
            @PathVariable String codeValue,
            @Valid @RequestBody DetailCodeRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.updateDetailCode(groupId, codeValue, request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
