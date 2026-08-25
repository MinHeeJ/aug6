package kr.ac.knue.commonfoundation.helpcontents;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
public class HelpContentManagementController {
    private final HelpContentManagementService service;

    public HelpContentManagementController(HelpContentManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/help-contents")
    public ApiResponse<HelpContentSearchResponse> listHelpContents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String screenId) {
        return ApiResponse.ok(service.listHelpContents(page, size, screenId));
    }

    @PutMapping("/api/admin/help-contents/{screenId}")
    public ApiResponse<HelpContentRow> saveHelpContent(@PathVariable String screenId,
            @Valid @RequestBody HelpContentSaveRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveHelpContent(screenId, request, currentUser(servletRequest).userId()));
    }

    @GetMapping("/api/help-contents/{screenId}")
    public ApiResponse<HelpContentResponse> getHelpContent(@PathVariable String screenId) {
        return ApiResponse.ok(service.getHelpContent(screenId));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
