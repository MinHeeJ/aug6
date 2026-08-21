package kr.ac.knue.commonfoundation.operations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommonOperationsService {
    private static final Set<String> REQUIRED_SETTING_KEYS = Set.of(
            "SESSION_IDLE_MINUTES",
            "PAGE_SIZE_DEFAULT",
            "DEFAULT_SEARCH_PERIOD_DAYS",
            "LARGE_QUERY_THRESHOLD",
            "LONG_RUNNING_TASK_THRESHOLD");
    private static final Map<String, String> DEFAULT_UNITS = Map.of(
            "SESSION_IDLE_MINUTES", "MINUTES",
            "PAGE_SIZE_DEFAULT", "ROWS",
            "DEFAULT_SEARCH_PERIOD_DAYS", "DAYS",
            "LARGE_QUERY_THRESHOLD", "COUNT",
            "LONG_RUNNING_TASK_THRESHOLD", "SECONDS");

    private final CommonOperationsMapper mapper;

    public CommonOperationsService(CommonOperationsMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<MenuExposureSetting> listMenuExposureSettings() {
        return mapper.listMenuExposureSettings();
    }

    @Transactional
    public List<MenuExposureSetting> saveMenuExposureSettings(MenuExposureSaveRequest request, Long currentUserId) {
        List<ValidationError> fields = new ArrayList<>();
        for (MenuExposureItem item : request.settings()) {
            if (item.exposureStartAt() != null && item.exposureEndAt() != null && item.exposureEndAt().isBefore(item.exposureStartAt())) {
                fields.add(new ValidationError("exposureEndAt", "노출 종료일시는 시작일시보다 빠를 수 없습니다."));
            }
            if (item.menuId() != null && mapper.existsMenu(item.menuId()) == 0) {
                fields.add(new ValidationError("menuId", "존재하지 않는 메뉴입니다."));
            }
        }
        rejectIf(fields, "메뉴 노출 설정 요청이 올바르지 않습니다.");
        for (MenuExposureItem item : request.settings()) {
            mapper.updateMenuExposure(item.menuId(), item.systemUseYn(), item.exposureStartAt(), item.exposureEndAt(), currentUserId, request.changeReason().trim());
        }
        return request.settings().stream().map(item -> mapper.findMenuExposure(item.menuId())).toList();
    }

    @Transactional(readOnly = true)
    public List<DetailCodeUsageSetting> listDetailCodeUsageSettings(String groupId) {
        return mapper.listDetailCodeUsageSettings(normalize(groupId));
    }

    @Transactional(readOnly = true)
    public List<DetailCodeUsageSetting> listActiveDetailCodeOptions(String groupId) {
        return mapper.listActiveDetailCodeOptions(normalize(groupId));
    }

    @Transactional
    public DetailCodeUsageSetting updateDetailCodeUsageSetting(String groupId, String codeValue, DetailCodeUsageUpdateRequest request, Long currentUserId) {
        String normalizedGroupId = normalize(groupId);
        String normalizedCodeValue = normalize(codeValue);
        List<ValidationError> fields = new ArrayList<>();
        if (mapper.existsDetailCode(normalizedGroupId, normalizedCodeValue) == 0) {
            fields.add(new ValidationError("codeValue", "등록된 상세코드만 사용기간을 변경할 수 있습니다."));
        }
        if (request.validStartDate() != null && request.validEndDate() != null && request.validEndDate().isBefore(request.validStartDate())) {
            fields.add(new ValidationError("validEndDate", "적용 종료일은 시작일보다 빠를 수 없습니다."));
        }
        rejectIf(fields, "상세코드 사용 설정 요청이 올바르지 않습니다.");
        mapper.updateDetailCodeUsage(normalizedGroupId, normalizedCodeValue, request.systemUseYn(), request.validStartDate(), request.validEndDate(), currentUserId, request.changeReason().trim());
        return mapper.findDetailCodeUsage(normalizedGroupId, normalizedCodeValue);
    }

    @Transactional(readOnly = true)
    public List<CommonSettingRow> listCommonSettings() {
        return mapper.listCommonSettings();
    }

    @Transactional
    public List<CommonSettingRow> saveCommonSettings(CommonSettingsSaveRequest request, Long currentUserId) {
        validateCommonSettings(request);
        for (CommonSettingInput input : request.settings()) {
            String key = normalize(input.getSettingKey());
            mapper.upsertCommonSetting(key, input.getSettingValue().trim(), defaultUnit(key, input.getSettingUnit()), currentUserId, request.changeReason().trim());
        }
        return mapper.listCommonSettings();
    }

    @Transactional(readOnly = true)
    public List<BaseYearSetting> listBaseYearSettings() {
        return mapper.listBaseYearSettings();
    }

    @Transactional
    public BaseYearSetting saveBaseYearSettings(BaseYearSaveRequest request, Long currentUserId) {
        validateYear(request.baseYear(), "baseYear");
        validateYear(request.currentEvaluationYear(), "currentEvaluationYear");
        validateYear(request.defaultSearchYear(), "defaultSearchYear");
        mapper.upsertBaseYearSetting(request.baseYear(), request.currentEvaluationYear(), request.defaultSearchYear(), request.copyRequestedYn(), request.initializeRequestedYn(), currentUserId, request.changeReason().trim());
        return mapper.findBaseYearSetting(request.baseYear());
    }

    @Transactional
    public StandardPreparationHistory prepareBaseYearStandards(Integer baseYear, StandardPreparationRequest request, Long currentUserId) {
        validateYear(baseYear, "baseYear");
        if (mapper.findBaseYearSetting(baseYear) == null) {
            mapper.upsertBaseYearSetting(baseYear, baseYear, baseYear, request.copyRequestedYn(), request.initializeRequestedYn(), currentUserId, request.changeReason().trim());
        }
        mapper.insertPreparationHistory(baseYear, request.copyRequestedYn(), request.initializeRequestedYn(), currentUserId, request.changeReason().trim());
        return mapper.latestPreparationHistory(baseYear);
    }

    @Transactional(readOnly = true)
    public DefaultSearchYearResponse getDefaultSearchYear() {
        Integer defaultSearchYear = mapper.findDefaultSearchYear();
        if (defaultSearchYear == null) {
            defaultSearchYear = LocalDate.now().getYear();
        }
        return new DefaultSearchYearResponse(defaultSearchYear);
    }

    private void validateCommonSettings(CommonSettingsSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (CommonSettingInput input : request.settings()) {
            input.getUnexpectedFields().forEach((field, ignored) -> {
                if ("userId".equals(field) || "businessScope".equals(field)) {
                    fields.add(new ValidationError(field, "공통 환경설정은 특정 사용자나 업무별 개별 scope를 저장하지 않습니다."));
                } else {
                    fields.add(new ValidationError(field, "공통 환경설정에서 허용하지 않는 필드입니다."));
                }
            });
            String key = normalize(input.getSettingKey());
            if (!REQUIRED_SETTING_KEYS.contains(key)) {
                fields.add(new ValidationError("settingKey", "허용된 공통 설정 항목 키가 아닙니다."));
            }
            if (!seen.add(key)) {
                fields.add(new ValidationError("settingKey", "같은 setting_key를 중복 저장할 수 없습니다."));
            }
            if (input.getSettingValue() == null || input.getSettingValue().isBlank()) {
                fields.add(new ValidationError("settingValue", "설정값은 필수입니다."));
            } else {
                try {
                    int value = Integer.parseInt(input.getSettingValue().trim());
                    if (value <= 0) fields.add(new ValidationError("settingValue", "설정값은 1 이상이어야 합니다."));
                    if ("PAGE_SIZE_DEFAULT".equals(key) && !Set.of(20, 50, 100).contains(value)) {
                        fields.add(new ValidationError("settingValue", "페이지당 조회건수는 20, 50, 100 중 하나여야 합니다."));
                    }
                } catch (NumberFormatException exception) {
                    fields.add(new ValidationError("settingValue", "설정값은 숫자여야 합니다."));
                }
            }
        }
        rejectIf(fields, "공통 환경설정 저장 요청이 올바르지 않습니다.");
    }

    private void validateYear(Integer year, String field) {
        if (year == null || year < 2000 || year > 2100) {
            throw new BusinessValidationException("기준연도 요청이 올바르지 않습니다.", List.of(new ValidationError(field, "연도는 2000~2100 범위여야 합니다.")));
        }
    }

    private void rejectIf(List<ValidationError> fields, String message) {
        if (!fields.isEmpty()) {
            throw new BusinessValidationException(message, fields);
        }
    }

    private String defaultUnit(String key, String unit) {
        return unit == null || unit.isBlank() ? DEFAULT_UNITS.get(key) : unit.trim().toUpperCase();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
