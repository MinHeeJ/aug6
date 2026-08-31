package kr.ac.knue.commonfoundation.basic33;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationAreaService {
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "evaluation_areas";
    private final EvaluationAreaMapper mapper;

    public EvaluationAreaService(EvaluationAreaMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationAreaSearchResponse list(EvaluationAreaSearchCriteria criteria) {
        return new EvaluationAreaSearchResponse(
                mapper.listEvaluationAreas(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countEvaluationAreas(criteria));
    }

    @Transactional
    public EvaluationAreaRow save(SaveEvaluationAreaRequest request, Long adminUserId, String requestId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("평가영역 저장 요청이 올바르지 않습니다.", fields);
        }
        String status = mapper.findRuleVersionStatus(request.ruleVersionId());
        if (status == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(status)) {
            throw new ConflictException("확정 또는 폐기된 규정버전의 평가영역은 수정할 수 없습니다.");
        }

        EvaluationAreaRow before = mapper.findByKey(request.ruleVersionId(), normalized(request.areaCode()));
        SaveEvaluationAreaRequest normalizedRequest = new SaveEvaluationAreaRequest(
                request.ruleVersionId(),
                normalized(request.areaCode()),
                request.areaName().trim(),
                request.sortOrder(),
                request.activeYn().trim(),
                request.periodApplyMethod().trim(),
                request.changeReason().trim());
        mapper.upsertEvaluationArea(normalizedRequest, adminUserId);
        EvaluationAreaRow after = mapper.findByKey(normalizedRequest.ruleVersionId(), normalizedRequest.areaCode());
        recordChangeHistory(before, after, normalizedRequest, adminUserId, requestId);
        return after;
    }

    private List<ValidationError> validate(SaveEvaluationAreaRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.ruleVersionId() == null) {
            fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        }
        if (!hasText(request.areaCode())) {
            fields.add(new ValidationError("areaCode", "평가영역 코드를 입력하세요."));
        }
        if (!hasText(request.areaName())) {
            fields.add(new ValidationError("areaName", "평가영역명을 입력하세요."));
        }
        if (request.sortOrder() == null) {
            fields.add(new ValidationError("sortOrder", "정렬순서를 입력하세요."));
        }
        if (!USE_FLAGS.contains(trim(request.activeYn()))) {
            fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        }
        if (!hasText(request.periodApplyMethod())) {
            fields.add(new ValidationError("periodApplyMethod", "평가기간 적용방식을 입력하세요."));
        }
        if (!hasText(request.changeReason())) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        return fields;
    }

    private void recordChangeHistory(
            EvaluationAreaRow before,
            EvaluationAreaRow after,
            SaveEvaluationAreaRequest request,
            Long adminUserId,
            String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.ruleVersionId() + ":" + request.areaCode();
        recordIfChanged(before == null ? null : before.areaName(), after.areaName(), "area_name", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : String.valueOf(before.sortOrder()), String.valueOf(after.sortOrder()), "sort_order", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.activeYn(), after.activeYn(), "active_yn", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.periodApplyMethod(), after.periodApplyMethod(), "period_apply_method", changeType, targetKey, adminUserId, request.changeReason(), requestId);
    }

    private void recordIfChanged(String beforeValue, String afterValue, String fieldName, String changeType,
                                 String targetKey, Long adminUserId, String changeReason, String requestId) {
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory(TARGET_BUSINESS, targetKey, changeType, fieldName, beforeValue, afterValue,
                    adminUserId, changeReason, requestId);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalized(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
