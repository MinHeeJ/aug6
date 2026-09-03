package kr.ac.knue.commonfoundation.basic46;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    public ApiResponse<FinalEvaluationConfirmationSearchResponse> listFinalEvaluationConfirmations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(required = false) String finalStatus,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.list(new FinalEvaluationConfirmationSearchCriteria(page, size, evaluationYear, targetUserId, finalStatus)));
    }

    @PostMapping("/api/business/final-evaluation-confirmations/{targetId}/transition")
    public ApiResponse<FinalEvaluationTransitionResult> saveFinalEvaluationConfirmationTransition(
            @PathVariable Long targetId,
            @Valid @RequestBody FinalEvaluationTransitionRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        if (request != null
                && request.actionType() != null
                && "CANCEL".equalsIgnoreCase(request.actionType().trim())
                && (request.cancelReason() == null || request.cancelReason().isBlank())) {
            throw new BusinessValidationException("최종평가 전이 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("cancelReason", "확정취소 사유를 입력하세요.")));
        }
        FinalEvaluationTransitionResult result = service.transition(targetId, request, user.userId());
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
