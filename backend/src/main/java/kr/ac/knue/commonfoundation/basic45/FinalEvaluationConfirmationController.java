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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FinalEvaluationConfirmationController {
    private final FinalEvaluationConfirmationService service;

    public FinalEvaluationConfirmationController(FinalEvaluationConfirmationService service) {
        this.service = service;
    }

    @GetMapping("/api/business/final-evaluation-confirmations")
    public ApiResponse<FinalEvaluationConfirmationListResponse> listFinalEvaluationConfirmations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String targetUserId,
            @RequestParam(required = false) String confirmationStatus,
            HttpServletRequest servletRequest) {
        requireAnyRole(servletRequest, List.of("R04", "R08", "R09"));
        return ApiResponse.ok(service.list(new FinalEvaluationConfirmationSearchCriteria(
                page, size, evaluationYear, areaCode, targetUserId, confirmationStatus)));
    }

    @PostMapping("/api/business/final-evaluation-confirmations/{targetId}/confirm")
    public ApiResponse<FinalEvaluationConfirmationResult> createFinalEvaluationConfirmation(
            @PathVariable Long targetId,
            @RequestBody(required = false) EvaluationBatchActionRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireAnyRole(servletRequest, List.of("R04", "R09"));
        String evaluationYear = requireEvaluationYear(request, "최종평가 확정 요청이 올바르지 않습니다.");
        FinalEvaluationConfirmationResult result = service.confirm(targetId, evaluationYear, user.userId());
        return ApiResponse.ok(result, result.requestId());
    }

    @PostMapping("/api/business/final-evaluation-confirmations/{targetId}/cancel")
    public ApiResponse<FinalEvaluationConfirmationResult> updateFinalEvaluationConfirmationCancel(
            @PathVariable Long targetId,
            @RequestBody(required = false) EvaluationBatchActionRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireAnyRole(servletRequest, List.of("R08", "R09"));
        String evaluationYear = requireEvaluationYear(request, "최종평가 확정취소 요청이 올바르지 않습니다.");
        validateCancelReason(request);
        FinalEvaluationConfirmationResult result = service.cancel(targetId, evaluationYear, request.cancelReason(), user.userId());
        return ApiResponse.ok(result, result.requestId());
    }

    private String requireEvaluationYear(EvaluationBatchActionRequest request, String message) {
        if (request == null || request.evaluationYear() == null || request.evaluationYear().isBlank()) {
            throw new BusinessValidationException(message, List.of(new ValidationError("evaluationYear", "평가연도를 입력하세요.")));
        }
        return request.evaluationYear();
    }

    private void validateCancelReason(EvaluationBatchActionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || request.cancelReason() == null || request.cancelReason().isBlank()) {
            fields.add(new ValidationError("cancelReason", "확정취소 사유를 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("최종평가 확정취소 요청이 올바르지 않습니다.", fields);
        }
    }

    private CurrentUser requireAnyRole(HttpServletRequest request, List<String> allowedRoles) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().stream().anyMatch(allowedRoles::contains)) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }
}
