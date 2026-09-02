package kr.ac.knue.commonfoundation.basic43;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantPaymentApprovalService {
    private static final Set<String> ACTIONS = Set.of("APPROVE", "REJECT", "CANCEL_APPROVAL");
    private final GrantPaymentApprovalMapper mapper;

    public GrantPaymentApprovalService(GrantPaymentApprovalMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public GrantPaymentApprovalSearchResponse list(GrantPaymentApprovalSearchCriteria criteria) {
        GrantPaymentApprovalSearchCriteria normalized = criteria == null
                ? new GrantPaymentApprovalSearchCriteria(0, 20, null, null, null)
                : criteria;
        return new GrantPaymentApprovalSearchResponse(
                mapper.listGrantPaymentApprovals(normalized),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.countGrantPaymentApprovals(normalized));
    }

    @Transactional
    public GrantPaymentApprovalRow transition(Long grantApplicationId, BusinessTransitionRequest request, Long userId) {
        List<ValidationError> fields = validate(grantApplicationId, request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("지급승인 처리 요청이 올바르지 않습니다.", fields);
        }
        String actionType = request.actionType().trim().toUpperCase();
        GrantPaymentApprovalRow current = mapper.findLatestByGrantApplicationId(grantApplicationId);
        if (current == null) {
            throw new NotFoundException("지급승인 처리 대상을 찾을 수 없습니다.");
        }
        if (mapper.paymentScopeExists(grantApplicationId, userId) == 0) {
            throw new ConflictException("지급승인 처리 권한 또는 데이터 범위가 없습니다.");
        }
        String previousStatus = current.nextStatus();
        String nextStatus = nextStatus(actionType, current);
        if (mapper.transitionAllowed(previousStatus, nextStatus) == 0) {
            throw new ConflictException("현재 상태에서 허용되지 않은 지급승인 처리입니다.");
        }
        String approvalStatus = approvalStatus(actionType);
        if ("REJECT".equals(actionType) && mapper.rejectionReasonExists(request.reasonCode().trim()) == 0) {
            throw new BusinessValidationException("등록된 반려사유를 선택하세요.", List.of(new ValidationError("reasonCode", "기존 반려사유를 선택하세요.")));
        }
        return mapper.insertTransition(
                grantApplicationId,
                current.linkedAchievementId(),
                current.evaluationYear(),
                approvalStatus,
                previousStatus,
                nextStatus,
                current.requestedAmountSnapshot(),
                current.paymentAmountSnapshot(),
                current.accountSnapshotRef(),
                trimToNull(request.reasonCode()),
                trimToNull(request.opinion()),
                userId,
                "지급승인 처리");
    }

    private String approvalStatus(String actionType) {
        return switch (actionType) {
            case "APPROVE" -> "APPROVED";
            case "REJECT" -> "REJECTED";
            case "CANCEL_APPROVAL" -> "APPROVAL_CANCELLED";
            default -> throw new ConflictException("허용되지 않은 처리구분입니다.");
        };
    }

    private String nextStatus(String actionType, GrantPaymentApprovalRow current) {
        return switch (actionType) {
            case "APPROVE" -> "CERTIFIED";
            case "REJECT" -> "CERTIFICATION_RETURNED";
            case "CANCEL_APPROVAL" -> {
                if (!"APPROVED".equals(current.approvalStatus())) {
                    throw new ConflictException("승인취소는 지급승인 상태에서만 처리할 수 있습니다.");
                }
                yield "SUBMITTED";
            }
            default -> throw new ConflictException("허용되지 않은 처리구분입니다.");
        };
    }

    private List<ValidationError> validate(Long grantApplicationId, BusinessTransitionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (grantApplicationId == null || grantApplicationId <= 0) {
            fields.add(new ValidationError("targetId", "처리 대상을 선택하세요."));
        }
        if (request == null || request.actionType() == null || request.actionType().isBlank()) {
            fields.add(new ValidationError("actionType", "처리구분을 선택하세요."));
            return fields;
        }
        String actionType = request.actionType().trim().toUpperCase();
        if (!ACTIONS.contains(actionType)) {
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
        return fields;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
