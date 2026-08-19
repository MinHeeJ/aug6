package kr.ac.knue.commonfoundation.organizations;

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
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/api/admin/organizations")
    public ApiResponse<List<OrganizationRow>> searchOrganizations(
            @RequestParam(required = false) String organizationCodeFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(organizationService.searchOrganizations(organizationCodeFilter, page, size));
    }

    @GetMapping("/api/admin/organizations/tree")
    public ApiResponse<List<OrganizationTreeNode>> getOrganizationTree() {
        return ApiResponse.ok(organizationService.getOrganizationTree());
    }

    @PutMapping("/api/admin/organizations/{organizationCode}/parent-relations")
    public ApiResponse<OrganizationRow> saveOrganizationParentRelation(
            @PathVariable String organizationCode,
            @Valid @RequestBody OrganizationParentRelationRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser(servletRequest);
        return ApiResponse.ok(organizationService.saveParentRelation(organizationCode, request, currentUser.userId()));
    }

    @GetMapping("/api/admin/organizations/{organizationCode}/parent-relations/history")
    public ApiResponse<List<OrganizationRelationHistoryRow>> listOrganizationParentRelationHistory(
            @PathVariable String organizationCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(organizationService.listHistory(organizationCode, page, size));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
