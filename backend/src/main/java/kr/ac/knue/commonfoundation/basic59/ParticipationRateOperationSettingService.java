package kr.ac.knue.commonfoundation.basic59;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipationRateOperationSettingService {
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "participation_rate_operation_settings";
    private final ParticipationRateOperationSettingMapper mapper;

    public ParticipationRateOperationSettingService(ParticipationRateOperationSettingMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ParticipationRateOperationSettingSearchResponse list(ParticipationRateOperationSettingSearchCriteria criteria) {
        return new ParticipationRateOperationSettingSearchResponse(
                mapper.listParticipationRateOperationSettings(criteria),
                criteria.safePage(),
                criteria.safeSize(),
                mapper.countParticipationRateOperationSettings(criteria));
    }

    @Transactional
    public ParticipationRateOperationSettingSaveResponse save(SaveParticipationRateOperationSettingRequest request, CurrentUser user, String requestId) {
        SaveParticipationRateOperationSettingRequest normalized = normalizeAndValidate(request);
        String status = mapper.findRuleVersionStatus(normalized.ruleVersionId());
        if (status == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(status)) {
            throw new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다.");
        }
        String evaluationYear = mapper.findRuleVersionEvaluationYear(normalized.ruleVersionId());
        if (evaluationYear == null || evaluationYear.isBlank()) {
            throw new NotFoundException("규정버전 적용연도를 찾을 수 없습니다.");
        }

        List<ParticipationRateOperationSettingRow> saved = new ArrayList<>();
        for (SaveParticipationRateOperationSettingRequest.Rate rate : normalized.rates()) {
            ParticipationRateOperationSettingRow before = mapper.findByBusinessKey(
                    normalized.ruleVersionId(), evaluationYear, normalized.achievementAreaCode(), normalized.achievementCategoryCode(),
                    normalized.managementItemCode(), rate.researcherCountBand(), rate.participationTypeCode());
            ParticipationRateOperationSettingRow after = mapper.upsertParticipationRateOperationSetting(normalized, rate, user.userId(), requestId);
            recordChangeHistory(before, after, normalized, rate, evaluationYear, user.userId(), requestId);
            saved.add(after);
        }
        return new ParticipationRateOperationSettingSaveResponse(saved);
    }

    private SaveParticipationRateOperationSettingRequest normalizeAndValidate(SaveParticipationRateOperationSettingRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String achievementAreaCode = normalized(request.achievementAreaCode());
        String achievementCategoryCode = normalized(request.achievementCategoryCode());
        String managementItemCode = normalized(request.managementItemCode());
        String changeReason = trim(request.changeReason());
        List<SaveParticipationRateOperationSettingRequest.Rate> rates = new ArrayList<>();

        if (request.ruleVersionId() == null) fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        if (!hasText(achievementAreaCode)) fields.add(new ValidationError("achievementAreaCode", "업적영역 코드를 입력하세요."));
        if (!hasText(achievementCategoryCode)) fields.add(new ValidationError("achievementCategoryCode", "업적분류 코드를 입력하세요."));
        if (!hasText(managementItemCode)) fields.add(new ValidationError("managementItemCode", "관리항목 코드를 입력하세요."));
        if (request.rates() == null || request.rates().isEmpty()) {
            fields.add(new ValidationError("rates", "배분율 matrix를 입력하세요."));
        } else {
            for (int index = 0; index < request.rates().size(); index++) {
                SaveParticipationRateOperationSettingRequest.Rate rate = request.rates().get(index);
                String researcherCountBand = normalized(rate.researcherCountBand());
                String participationTypeCode = normalized(rate.participationTypeCode());
                BigDecimal distributionRate = rate.distributionRate();
                if (!hasText(researcherCountBand)) fields.add(new ValidationError("rates[" + index + "].researcherCountBand", "연구자 수 구간을 입력하세요."));
                if (!hasText(participationTypeCode)) fields.add(new ValidationError("rates[" + index + "].participationTypeCode", "참여구분 코드를 입력하세요."));
                if (distributionRate == null) {
                    fields.add(new ValidationError("rates[" + index + "].distributionRate", "배분율을 입력하세요."));
                } else if (distributionRate.compareTo(BigDecimal.ZERO) < 0 || distributionRate.compareTo(new BigDecimal("100.00")) > 0) {
                    fields.add(new ValidationError("rates[" + index + "].distributionRate", "배분율은 0 이상 100 이하이어야 합니다."));
                }
                rates.add(new SaveParticipationRateOperationSettingRequest.Rate(researcherCountBand, participationTypeCode, distributionRate));
            }
        }
        if (!hasText(changeReason)) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("참여구분별 배분율 저장 요청이 올바르지 않습니다.", fields);

        return new SaveParticipationRateOperationSettingRequest(request.ruleVersionId(), achievementAreaCode,
                achievementCategoryCode, managementItemCode, List.copyOf(rates), changeReason);
    }

    private void recordChangeHistory(ParticipationRateOperationSettingRow before,
                                     ParticipationRateOperationSettingRow after,
                                     SaveParticipationRateOperationSettingRequest request,
                                     SaveParticipationRateOperationSettingRequest.Rate rate,
                                     String evaluationYear,
                                     Long userId,
                                     String requestId) {
        String beforeValue = before == null ? null : summary(before);
        String afterValue = summary(after);
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory(TARGET_BUSINESS, targetKey(request, rate, evaluationYear), before == null ? "CREATE" : "UPDATE",
                    "distributionRate", beforeValue, afterValue, userId, request.changeReason(), requestId);
        }
    }

    private String targetKey(SaveParticipationRateOperationSettingRequest request,
                             SaveParticipationRateOperationSettingRequest.Rate rate,
                             String evaluationYear) {
        return request.ruleVersionId() + ":" + evaluationYear + ":" + request.achievementAreaCode() + ":" +
                request.achievementCategoryCode() + ":" + request.managementItemCode() + ":" +
                rate.researcherCountBand() + ":" + rate.participationTypeCode();
    }

    private String summary(ParticipationRateOperationSettingRow row) {
        return row.ruleVersionId() + ":" + row.evaluationYear() + ":" + row.managementItemCode() + ":" +
                row.researcherCountBand() + ":" + row.participationTypeCode() + ":" + row.distributionRate() + ":" + row.activeYn();
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String normalized(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed.toUpperCase();
    }
}
