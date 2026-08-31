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
public class EvaluationItemService {
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "evaluation_items";
    private final EvaluationItemMapper mapper;

    public EvaluationItemService(EvaluationItemMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationItemSearchResponse list(EvaluationItemSearchCriteria criteria) {
        return new EvaluationItemSearchResponse(
                mapper.listEvaluationItems(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countEvaluationItems(criteria));
    }

    @Transactional
    public EvaluationItemRow save(SaveEvaluationItemRequest request, Long adminUserId, String requestId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("평가항목 저장 요청이 올바르지 않습니다.", fields);
        }
        String status = mapper.findRuleVersionStatus(request.ruleVersionId());
        if (status == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(status)) {
            throw new ConflictException("확정 또는 폐기된 규정버전의 평가항목은 수정할 수 없습니다.");
        }

        SaveEvaluationItemRequest normalizedRequest = new SaveEvaluationItemRequest(
                request.ruleVersionId(),
                normalized(request.areaCode()),
                normalized(request.itemCode()),
                request.itemName().trim(),
                optionalNormalized(request.parentItemCode()),
                request.sortOrder(),
                request.activeYn().trim(),
                request.scoreApplyMethod().trim(),
                request.changeReason().trim());
        if (Objects.equals(normalizedRequest.itemCode(), normalizedRequest.parentItemCode())) {
            throw new BusinessValidationException("상위 평가항목은 자기 자신일 수 없습니다.", List.of(new ValidationError("parentItemCode", "상위 평가항목은 자기 자신일 수 없습니다.")));
        }
        Long areaId = mapper.findAreaId(normalizedRequest.ruleVersionId(), normalizedRequest.areaCode());
        if (areaId == null) {
            throw new NotFoundException("평가영역을 찾을 수 없습니다.");
        }
        if (normalizedRequest.parentItemCode() != null && mapper.existsParentItem(areaId, normalizedRequest.parentItemCode()) == 0) {
            throw new BusinessValidationException("상위 평가항목을 찾을 수 없습니다.", List.of(new ValidationError("parentItemCode", "같은 평가영역의 상위 평가항목을 선택하세요.")));
        }

        EvaluationItemRow before = mapper.findByKey(areaId, normalizedRequest.itemCode());
        mapper.upsertEvaluationItem(normalizedRequest, areaId, adminUserId);
        EvaluationItemRow after = mapper.findByKey(areaId, normalizedRequest.itemCode());
        recordChangeHistory(before, after, normalizedRequest, adminUserId, requestId);
        return after;
    }

    private List<ValidationError> validate(SaveEvaluationItemRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.ruleVersionId() == null) {
            fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        }
        if (!hasText(request.areaCode())) {
            fields.add(new ValidationError("areaCode", "평가영역 코드를 입력하세요."));
        }
        if (!hasText(request.itemCode())) {
            fields.add(new ValidationError("itemCode", "평가항목 코드를 입력하세요."));
        }
        if (!hasText(request.itemName())) {
            fields.add(new ValidationError("itemName", "평가항목명을 입력하세요."));
        }
        if (request.sortOrder() == null) {
            fields.add(new ValidationError("sortOrder", "정렬순서를 입력하세요."));
        }
        if (!USE_FLAGS.contains(trim(request.activeYn()))) {
            fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        }
        if (!hasText(request.scoreApplyMethod())) {
            fields.add(new ValidationError("scoreApplyMethod", "배점 적용방식을 입력하세요."));
        }
        if (!hasText(request.changeReason())) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        return fields;
    }

    private void recordChangeHistory(
            EvaluationItemRow before,
            EvaluationItemRow after,
            SaveEvaluationItemRequest request,
            Long adminUserId,
            String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.ruleVersionId() + ":" + request.areaCode() + ":" + request.itemCode();
        recordIfChanged(before == null ? null : before.itemName(), after.itemName(), "item_name", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.parentItemCode(), after.parentItemCode(), "parent_item_code", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : String.valueOf(before.sortOrder()), String.valueOf(after.sortOrder()), "sort_order", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.activeYn(), after.activeYn(), "active_yn", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.scoreApplyMethod(), after.scoreApplyMethod(), "score_apply_method", changeType, targetKey, adminUserId, request.changeReason(), requestId);
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

    private String optionalNormalized(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
