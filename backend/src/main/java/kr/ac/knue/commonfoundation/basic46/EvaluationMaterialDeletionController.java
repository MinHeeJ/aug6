package kr.ac.knue.commonfoundation.basic46;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvaluationMaterialDeletionController {
    private final EvaluationMaterialDeletionService service;

    public EvaluationMaterialDeletionController(EvaluationMaterialDeletionService service) {
        this.service = service;
    }

    @GetMapping("/api/business/evaluation-material-deletions/preview")
    public ApiResponse<EvaluationMaterialDeletionPreviewResponse> previewEvaluationMaterialDeletion(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String generationBatchId,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.preview(new EvaluationMaterialDeletionSearchCriteria(
                page, size, evaluationYear, areaCode, generationBatchId)));
    }

    @PostMapping("/api/business/evaluation-material-deletions")
    public ApiResponse<EvaluationMaterialDeletionResult> createEvaluationMaterialDeletion(
            @Valid @RequestBody EvaluationMaterialDeletionRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        EvaluationMaterialDeletionResult result = service.delete(request, user.userId());
        return ApiResponse.ok(result, result.requestId());
    }

    private CurrentUser requireR09(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R09")) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }
}
