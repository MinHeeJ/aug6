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
public class DepartmentChairConfirmationService {
    private static final Set<String> ACTIONS = Set.of("CONFIRM", "REJECT");
    private final DepartmentChairConfirmationMapper mapper;

    public DepartmentChairConfirmationService(DepartmentChairConfirmationMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DepartmentChairConfirmationSearchResponse list(DepartmentChairConfirmationSearchCriteria criteria) {
        DepartmentChairConfirmationSearchCriteria normalized = criteria == null
                ? new DepartmentChairConfirmationSearchCriteria(0, 20, null, null, null, null)
                : criteria;
        return new DepartmentChairConfirmationSearchResponse(
                mapper.listDepartmentChairConfirmTargets(normalized),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.countDepartmentChairConfirmTargets(normalized));
    }

    @Transactional
    public DepartmentChairConfirmationRow transition(Long achievementId, BusinessTransitionRequest request, Long userId) {
        List<ValidationError> fields = validate(achievementId, request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("학과장 확인 처리 요청이 올바르지 않습니다.", fields);
        }
        String actionType = request.actionType().trim().toUpperCase();
        DepartmentChairConfirmationRow current = mapper.findLatestByAchievementId(achievementId);
        if (current == null) {
            throw new NotFoundException("학과장 확인 처리 대상을 찾을 수 없습니다.");
        }
        String nextStatus = "CONFIRM".equals(actionType) ? "DEPARTMENT_CONFIRMED" : "DEPARTMENT_REJECTED";
        if (mapper.activeDepartmentChairConfirmPeriodExists(current.evaluationYear(), current.departmentOrganizationCode(), current.areaCode()) == 0) {
            throw new ConflictException("학과장 확인기간 안의 대상만 처리할 수 있습니다.");
        }
        if (!"SUBMITTED".equals(current.nextStatus()) || mapper.transitionAllowed("SUBMITTED", nextStatus) == 0) {
            throw new ConflictException("제출 상태 업적만 학과장 확인 또는 미승인 처리할 수 있습니다.");
        }
        if ("REJECT".equals(actionType) && mapper.rejectionReasonExists(request.reasonCode().trim()) == 0) {
            throw new BusinessValidationException("등록된 반려사유를 선택하세요.", List.of(new ValidationError("reasonCode", "기존 반려사유를 선택하세요.")));
        }
        return mapper.insertTransition(
                achievementId,
                current.evaluationYear(),
                current.departmentOrganizationCode(),
                current.areaCode(),
                nextStatus,
                "SUBMITTED",
                nextStatus,
                trimToNull(request.opinion()),
                trimToNull(request.reasonCode()),
                userId,
                "학과장 확인 처리");
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
            fields.add(new ValidationError("actionType", "CONFIRM 또는 REJECT 중 하나를 선택하세요."));
        }
        if ("REJECT".equals(actionType)) {
            if (request.reasonCode() == null || request.reasonCode().isBlank()) {
                fields.add(new ValidationError("reasonCode", "미승인 사유를 선택하세요."));
            }
            if (request.opinion() == null || request.opinion().isBlank()) {
                fields.add(new ValidationError("opinion", "미승인 의견을 입력하세요."));
            }
        }
        return fields;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
