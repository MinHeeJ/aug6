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
public class AchievementVerificationService {
    private static final Set<String> ACTIONS = Set.of("CERTIFY", "RETURN", "CANCEL_CERTIFICATION");
    private final AchievementVerificationMapper mapper;

    public AchievementVerificationService(AchievementVerificationMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AchievementVerificationSearchResponse list(AchievementVerificationSearchCriteria criteria) {
        AchievementVerificationSearchCriteria normalized = criteria == null
                ? new AchievementVerificationSearchCriteria(0, 20, null, null, null)
                : criteria;
        return new AchievementVerificationSearchResponse(
                mapper.listAchievementVerificationTargets(normalized),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.countAchievementVerificationTargets(normalized));
    }

    @Transactional
    public AchievementVerificationRow transition(Long achievementId, BusinessTransitionRequest request, Long userId) {
        List<ValidationError> fields = validate(achievementId, request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("담당자 인증 처리 요청이 올바르지 않습니다.", fields);
        }
        String actionType = request.actionType().trim().toUpperCase();
        AchievementVerificationRow current = mapper.findLatestByAchievementId(achievementId);
        if (current == null) {
            throw new NotFoundException("담당자 인증 처리 대상을 찾을 수 없습니다.");
        }
        if (mapper.handlerScopeExists(achievementId, userId) == 0) {
            throw new ConflictException("담당 범위 안의 실적만 처리할 수 있습니다.");
        }
        String previousStatus = current.nextStatus();
        String nextStatus = nextStatus(actionType, current);
        if ("CANCEL_CERTIFICATION".equals(actionType)) {
            if (!"CERTIFIED".equals(current.nextStatus()) || !"DEPARTMENT_CONFIRMED".equals(current.previousStatus())) {
                throw new ConflictException("인증취소는 인증 상태에서 직전 검토 상태로만 복귀할 수 있습니다.");
            }
        } else {
            if (!"DEPARTMENT_CONFIRMED".equals(current.nextStatus()) || mapper.transitionAllowed("DEPARTMENT_CONFIRMED", nextStatus) == 0) {
                throw new ConflictException("학과장확인 상태 실적만 인증 또는 반려 처리할 수 있습니다.");
            }
        }
        if ("RETURN".equals(actionType) && mapper.rejectionReasonExists(request.reasonCode().trim()) == 0) {
            throw new BusinessValidationException("등록된 반려사유를 선택하세요.", List.of(new ValidationError("reasonCode", "기존 반려사유를 선택하세요.")));
        }
        return mapper.insertTransition(
                achievementId,
                current.evaluationYear(),
                userId,
                actionType,
                previousStatus,
                nextStatus,
                trimToNull(request.opinion()),
                trimToNull(request.evidenceRef()),
                trimToNull(request.reasonCode()),
                userId,
                "담당자 인증 처리");
    }

    private String nextStatus(String actionType, AchievementVerificationRow current) {
        return switch (actionType) {
            case "CERTIFY" -> "CERTIFIED";
            case "RETURN" -> "CERTIFICATION_RETURNED";
            case "CANCEL_CERTIFICATION" -> current.previousStatus();
            default -> throw new ConflictException("허용되지 않은 처리구분입니다.");
        };
    }

    private List<ValidationError> validate(Long achievementId, BusinessTransitionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (achievementId == null || achievementId <= 0) {
            fields.add(new ValidationError("targetId", "처리 대상을 선택하세요."));
        }
        if (request == null || request.actionType() == null || request.actionType().isBlank()) {
            fields.add(new ValidationError("actionType", "처리구분을 선택하세요."));
            return fields;
        }
        String actionType = request.actionType().trim().toUpperCase();
        if (!ACTIONS.contains(actionType)) {
            fields.add(new ValidationError("actionType", "CERTIFY, RETURN, CANCEL_CERTIFICATION 중 하나를 선택하세요."));
        }
        if (request.evidenceRef() == null || request.evidenceRef().isBlank()) {
            fields.add(new ValidationError("evidenceRef", "처리 근거를 입력하세요."));
        }
        if ("RETURN".equals(actionType)) {
            if (request.reasonCode() == null || request.reasonCode().isBlank()) {
                fields.add(new ValidationError("reasonCode", "인증반려 사유를 선택하세요."));
            }
            if (request.opinion() == null || request.opinion().isBlank()) {
                fields.add(new ValidationError("opinion", "인증반려 의견을 입력하세요."));
            }
        }
        return fields;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
