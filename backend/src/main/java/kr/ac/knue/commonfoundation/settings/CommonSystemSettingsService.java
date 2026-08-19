package kr.ac.knue.commonfoundation.settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommonSystemSettingsService {
    private final CommonSystemSettingsMapper mapper;

    public CommonSystemSettingsService(CommonSystemSettingsMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CommonSystemSettingsResponse getCommonSystemSettings() {
        return new CommonSystemSettingsResponse(mapper.listCommonSystemSettings(CommonSystemSettingKey.orderedKeys()));
    }

    @Transactional
    public CommonSystemSettingsResponse saveCommonSystemSettings(CommonSystemSettingsRequest request, Long adminUserId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("공통 환경설정 저장 요청이 올바르지 않습니다.", fields);
        }
        for (CommonSystemSettingsRequest.Item item : request.settings()) {
            mapper.upsertCommonSystemSetting(item.settingKey(), item.settingValue().trim(), item.unit().trim(), adminUserId, item.changeReason());
        }
        return getCommonSystemSettings();
    }

    private List<ValidationError> validate(CommonSystemSettingsRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || request.settings() == null || request.settings().isEmpty()) {
            fields.add(new ValidationError("settings", "저장할 공통 환경설정을 선택하세요."));
            return fields;
        }
        Set<String> seenKeys = new HashSet<>();
        for (int index = 0; index < request.settings().size(); index++) {
            CommonSystemSettingsRequest.Item item = request.settings().get(index);
            String prefix = "settings[" + index + "].";
            if (!CommonSystemSettingKey.isAllowed(item.settingKey())) {
                fields.add(new ValidationError(prefix + "settingKey", "허용된 공통 환경설정 항목만 저장할 수 있습니다."));
            } else if (!seenKeys.add(item.settingKey())) {
                fields.add(new ValidationError(prefix + "settingKey", "같은 설정 항목을 중복 저장할 수 없습니다."));
            }
            if (item.userId() != null) {
                fields.add(new ValidationError(prefix + "userId", "사용자별 개별 환경값은 생성할 수 없습니다."));
            }
            if (item.settingValue() == null || item.settingValue().isBlank()) {
                fields.add(new ValidationError(prefix + "settingValue", "설정값을 입력하세요."));
            } else if (!item.settingValue().trim().matches("[1-9][0-9]*")) {
                fields.add(new ValidationError(prefix + "settingValue", "단위·범위 확정 전 OQ-SET-001 기준에 따라 양의 정수값만 저장할 수 있습니다."));
            }
            if (item.unit() == null || item.unit().isBlank()) {
                fields.add(new ValidationError(prefix + "unit", "단위 OQ-SET-001 표시를 위해 단위를 입력하세요."));
            }
            if (item.changeReason() == null || item.changeReason().isBlank()) {
                fields.add(new ValidationError(prefix + "changeReason", "변경 사유를 입력하세요."));
            }
        }
        return fields;
    }
}
