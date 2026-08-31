package kr.ac.knue.commonfoundation.basic34;

import java.math.BigDecimal;
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
public class ParticipationRateService {
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "participation_rate_rules";
    private final ParticipationRateMapper mapper;

    public ParticipationRateService(ParticipationRateMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ParticipationRateSearchResponse list(ParticipationRateSearchCriteria criteria) {
        return new ParticipationRateSearchResponse(
                mapper.listParticipationRates(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countParticipationRates(criteria));
    }

    @Transactional
    public ParticipationRateRow save(SaveParticipationRateRequest request, Long userId, String requestId) {
        SaveParticipationRateRequest normalized = normalizeAndValidate(request);
        String status = mapper.findRuleVersionStatus(normalized.ruleVersionId());
        if (status == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(status)) {
            throw new ConflictException("확정 또는 폐기된 규정버전의 배분율은 수정할 수 없습니다.");
        }
        if (!Boolean.TRUE.equals(mapper.managementItemBelongsToRuleVersion(normalized.ruleVersionId(), normalized.managementItemId()))) {
            throw new NotFoundException("규정버전에 속한 관리항목을 찾을 수 없습니다.");
        }

        ParticipationRateRow before = mapper.findByKey(normalized);
        mapper.upsertParticipationRate(normalized, userId);
        ParticipationRateRow after = mapper.findByKey(normalized);
        recordChangeHistory(before, after, normalized, userId, requestId);
        return after;
    }

    private SaveParticipationRateRequest normalizeAndValidate(SaveParticipationRateRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.ruleVersionId() == null) fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        if (request.managementItemId() == null) fields.add(new ValidationError("managementItemId", "관리항목을 선택하세요."));
        if (request.researcherCount() == null) {
            fields.add(new ValidationError("researcherCount", "연구자 수를 입력하세요."));
        } else if (request.researcherCount() < 1) {
            fields.add(new ValidationError("researcherCount", "연구자 수는 1 이상이어야 합니다."));
        }
        if (!hasText(request.participationType())) fields.add(new ValidationError("participationType", "참여구분을 입력하세요."));
        if (request.distributionRate() == null) {
            fields.add(new ValidationError("distributionRate", "배분율을 입력하세요."));
        } else if (request.distributionRate().compareTo(BigDecimal.ZERO) < 0 || request.distributionRate().compareTo(BigDecimal.ONE) > 0) {
            fields.add(new ValidationError("distributionRate", "배분율은 0 이상 1 이하이어야 합니다."));
        }
        if (request.effectiveStartDate() == null) fields.add(new ValidationError("effectiveStartDate", "적용시작일을 입력하세요."));
        if (request.effectiveEndDate() == null) fields.add(new ValidationError("effectiveEndDate", "적용종료일을 입력하세요."));
        if (request.effectiveStartDate() != null && request.effectiveEndDate() != null
                && request.effectiveEndDate().isBefore(request.effectiveStartDate())) {
            fields.add(new ValidationError("effectiveEndDate", "적용종료일은 시작일 이후여야 합니다."));
        }
        if (!USE_FLAGS.contains(trim(request.activeYn()))) fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        if (!hasText(request.changeReason())) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("배분율 저장 요청이 올바르지 않습니다.", fields);
        }
        return new SaveParticipationRateRequest(
                request.ruleVersionId(),
                request.managementItemId(),
                request.researcherCount(),
                normalized(request.participationType()),
                request.distributionRate(),
                request.effectiveStartDate(),
                request.effectiveEndDate(),
                request.activeYn().trim(),
                request.changeReason().trim());
    }

    private void recordChangeHistory(ParticipationRateRow before, ParticipationRateRow after, SaveParticipationRateRequest request,
                                     Long userId, String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.ruleVersionId() + ":" + request.managementItemId() + ":" + request.researcherCount()
                + ":" + request.participationType() + ":" + request.effectiveStartDate() + ":" + request.effectiveEndDate();
        recordIfChanged(before == null ? null : before.distributionRate(), after.distributionRate(), "distribution_rate", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.activeYn(), after.activeYn(), "active_yn", changeType, targetKey, userId, request.changeReason(), requestId);
    }

    private void recordIfChanged(Object beforeValue, Object afterValue, String fieldName, String changeType,
                                 String targetKey, Long userId, String changeReason, String requestId) {
        String beforeString = beforeValue == null ? null : beforeValue.toString();
        String afterString = afterValue == null ? null : afterValue.toString();
        if (!Objects.equals(beforeString, afterString)) {
            mapper.insertChangeHistory(TARGET_BUSINESS, targetKey, changeType, fieldName, beforeString, afterString,
                    userId, changeReason, requestId);
        }
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String normalized(String value) { return value == null ? null : value.trim().toUpperCase(); }
}
