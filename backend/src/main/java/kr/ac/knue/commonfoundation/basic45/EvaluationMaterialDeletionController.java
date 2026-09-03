package kr.ac.knue.commonfoundation.basic45;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
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
    public ApiResponse<EvaluationMaterialDeletionPreviewResponse> listEvaluationMaterialDeletionTargets(
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
            @RequestBody(required = false) EvaluationBatchActionRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        validateDeleteRequest(request);
        EvaluationMaterialDeletionResult result = service.delete(request, user.userId());
        return ApiResponse.ok(result, result.requestId());
    }

    private void validateDeleteRequest(EvaluationBatchActionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || request.evaluationYear() == null || request.evaluationYear().isBlank()) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        }
        if (request == null || request.generationBatchId() == null || request.generationBatchId().isBlank()) {
            fields.add(new ValidationError("generationBatchId", "생성배치ID를 입력하세요."));
        }
        if (request == null || request.deleteReason() == null || request.deleteReason().isBlank()) {
            fields.add(new ValidationError("deleteReason", "삭제사유를 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("평가자료 삭제 요청이 올바르지 않습니다.", fields);
        }
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
