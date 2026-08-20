package kr.ac.knue.commonfoundation.evaluationyears;

import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationYearManagementService {
    private final EvaluationYearMapper mapper;

    public EvaluationYearManagementService(EvaluationYearMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationYearSettingsResponse getEvaluationYearSettings() {
        EvaluationYearSettingsRow settings = mapper.getEvaluationYearSettings();
        List<EvaluationYearPreparationRow> preparations = mapper.listEvaluationYearPreparations();
        if (settings == null) {
            return new EvaluationYearSettingsResponse(null, null, preparations, null, null, null);
        }
        return new EvaluationYearSettingsResponse(settings.currentEvaluationYear(), settings.defaultSearchYear(),
                preparations, settings.updatedBy(), settings.updatedAt(), settings.changeReason());
    }

    @Transactional
    public EvaluationYearSettingsResponse saveEvaluationYearSettings(EvaluationYearSettingsRequest request, Long adminUserId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("기준연도 설정 저장 요청이 올바르지 않습니다.", fields);
        }
        mapper.upsertEvaluationYearSettings(request.currentEvaluationYear(), request.defaultSearchYear(), adminUserId,
                request.changeReason());
        if (request.preparations() != null) {
            for (EvaluationYearSettingsRequest.Preparation preparation : request.preparations()) {
                mapper.upsertEvaluationYearPreparation(preparation.targetYear(), preparation.copyRequestedYn(),
                        preparation.resetRequestedYn(), adminUserId, preparation.changeReason());
            }
        }
        return getEvaluationYearSettings();
    }

    private List<ValidationError> validate(EvaluationYearSettingsRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("request", "기준연도 설정 요청 본문을 입력하세요."));
            return fields;
        }
        validateYear("currentEvaluationYear", request.currentEvaluationYear(), fields);
        validateYear("defaultSearchYear", request.defaultSearchYear(), fields);
        if (request.changeReason() == null || request.changeReason().isBlank()) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        if (request.preparations() == null) {
            return fields;
        }
        for (int index = 0; index < request.preparations().size(); index++) {
            EvaluationYearSettingsRequest.Preparation preparation = request.preparations().get(index);
            String prefix = "preparations[" + index + "].";
            validateYear(prefix + "targetYear", preparation.targetYear(), fields);
            if (!isYn(preparation.copyRequestedYn())) {
                fields.add(new ValidationError(prefix + "copyRequestedYn", "복사 여부는 Y 또는 N만 선택할 수 있습니다."));
            }
            if (!isYn(preparation.resetRequestedYn())) {
                fields.add(new ValidationError(prefix + "resetRequestedYn", "초기화 여부는 Y 또는 N만 선택할 수 있습니다."));
            }
            if ("Y".equals(preparation.copyRequestedYn()) && "Y".equals(preparation.resetRequestedYn())) {
                fields.add(new ValidationError(prefix + "resetRequestedYn", "복사와 초기화는 동시에 요청할 수 없습니다."));
            }
            if (preparation.changeReason() == null || preparation.changeReason().isBlank()) {
                fields.add(new ValidationError(prefix + "changeReason", "대상연도 준비 상태 변경 사유를 입력하세요."));
            }
        }
        return fields;
    }

    private void validateYear(String field, Integer year, List<ValidationError> fields) {
        if (year == null) {
            fields.add(new ValidationError(field, "연도를 입력하세요."));
            return;
        }
        if (year < 1900 || year > 9999) {
            fields.add(new ValidationError(field, "연도는 4자리 값으로 입력하세요."));
        }
    }

    private boolean isYn(String value) {
        return "Y".equals(value) || "N".equals(value);
    }
}
