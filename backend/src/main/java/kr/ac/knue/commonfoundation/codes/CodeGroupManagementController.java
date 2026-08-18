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
public class CodeGroupManagementController {
    private final CodeGroupManagementService service;

    public CodeGroupManagementController(CodeGroupManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/code-groups")
    public ApiResponse<List<CodeGroupRow>> listCodeGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String groupIdFilter,
            @RequestParam(required = false) String filter) {
        return ApiResponse.ok(service.listCodeGroups(page, size, groupIdFilter, filter));
    }

    @PostMapping("/api/admin/code-groups")
    public ApiResponse<CodeGroupRow> createCodeGroup(
            @Valid @RequestBody CodeGroupRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.createCodeGroup(request, currentUser(servletRequest).userId()));
    }

    @PutMapping("/api/admin/code-groups/{groupId}")
    public ApiResponse<CodeGroupRow> updateCodeGroup(
            @PathVariable String groupId,
            @Valid @RequestBody CodeGroupRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.updateCodeGroup(groupId, request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
