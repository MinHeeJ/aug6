package kr.ac.knue.commonfoundation.basic43;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
public class GrantPaymentApprovalController {
    private final GrantPaymentApprovalService service;

    public GrantPaymentApprovalController(GrantPaymentApprovalService service) {
        this.service = service;
    }

    @GetMapping("/api/business/grant-payment-approvals")
    public ApiResponse<GrantPaymentApprovalSearchResponse> listGrantPaymentApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String applicantName,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.list(new GrantPaymentApprovalSearchCriteria(
                page, size, evaluationYear, approvalStatus, applicantName)));
    }

    @PostMapping("/api/business/grant-payment-approvals/{targetId}/transition")
    public ApiResponse<GrantPaymentApprovalRow> saveGrantPaymentApprovalsTransition(
            @PathVariable String targetId,
            @Valid @RequestBody BusinessTransitionRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        Long grantApplicationId;
        try {
            grantApplicationId = Long.valueOf(targetId);
        } catch (NumberFormatException exception) {
            grantApplicationId = -1L;
        }
        validateTransitionRequest(request);
        return ApiResponse.ok(service.transition(grantApplicationId, request, user.userId()));
    }

    private void validateTransitionRequest(BusinessTransitionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String actionType = request == null || request.actionType() == null ? "" : request.actionType().trim().toUpperCase();
        if (!List.of("APPROVE", "REJECT", "CANCEL_APPROVAL").contains(actionType)) {
            fields.add(new ValidationError("actionType", "APPROVE, REJECT, CANCEL_APPROVAL 중 하나를 선택하세요."));
        }
        if ("REJECT".equals(actionType)) {
            if (request.reasonCode() == null || request.reasonCode().isBlank()) {
                fields.add(new ValidationError("reasonCode", "지급반려 사유를 선택하세요."));
            }
            if (request.opinion() == null || request.opinion().isBlank()) {
                fields.add(new ValidationError("opinion", "지급반려 의견을 입력하세요."));
            }
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("지급승인 처리 요청이 올바르지 않습니다.", fields);
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
