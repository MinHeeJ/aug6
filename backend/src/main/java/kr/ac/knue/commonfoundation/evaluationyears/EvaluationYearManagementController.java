package kr.ac.knue.commonfoundation.evaluationyears;

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
public class EvaluationYearManagementController {
    private final EvaluationYearManagementService service;

    public EvaluationYearManagementController(EvaluationYearManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/system-settings/evaluation-years")
    public ApiResponse<EvaluationYearSettingsResponse> getEvaluationYearSettings() {
        return ApiResponse.ok(service.getEvaluationYearSettings());
    }

    @PutMapping("/api/admin/system-settings/evaluation-years")
    public ApiResponse<EvaluationYearSettingsResponse> saveEvaluationYearSettings(@Valid @RequestBody EvaluationYearSettingsRequest request,
                                                                                  HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveEvaluationYearSettings(request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
