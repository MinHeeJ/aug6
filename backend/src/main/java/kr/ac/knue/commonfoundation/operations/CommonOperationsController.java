package kr.ac.knue.commonfoundation.operations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommonOperationsController {
    private final CommonOperationsService service;

    public CommonOperationsController(CommonOperationsService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/menus/exposure")
    public ApiResponse<List<MenuExposureSetting>> listMenuExposureSettings() {
        return ApiResponse.ok(service.listMenuExposureSettings());
    }

    @PutMapping("/api/admin/menus/exposure-save")
    public ApiResponse<List<MenuExposureSetting>> saveMenuExposureSettings(@Valid @RequestBody MenuExposureSaveRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveMenuExposureSettings(request, currentUser(servletRequest).userId()));
    }

    @GetMapping("/api/admin/code-groups/{groupId}/codes-usage")
    public ApiResponse<List<DetailCodeUsageSetting>> listDetailCodeUsageSettings(@PathVariable String groupId) {
        return ApiResponse.ok(service.listDetailCodeUsageSettings(groupId));
    }

    @GetMapping("/api/code-groups/{groupId}/codes/options")
    public ApiResponse<List<DetailCodeUsageSetting>> listActiveDetailCodeOptions(@PathVariable String groupId) {
        return ApiResponse.ok(service.listActiveDetailCodeOptions(groupId));
    }

    @PutMapping("/api/admin/code-groups/{groupId}/codes/{codeValue}/usage")
    public ApiResponse<DetailCodeUsageSetting> updateDetailCodeUsageSetting(@PathVariable String groupId,
            @PathVariable String codeValue,
            @Valid @RequestBody DetailCodeUsageUpdateRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.updateDetailCodeUsageSetting(groupId, codeValue, request, currentUser(servletRequest).userId()));
    }

    @GetMapping("/api/admin/system-settings/common")
    public ApiResponse<List<CommonSettingRow>> listCommonSettings() {
        return ApiResponse.ok(service.listCommonSettings());
    }

    @PutMapping("/api/admin/system-settings/common-values")
    public ApiResponse<List<CommonSettingRow>> saveCommonSettings(@Valid @RequestBody CommonSettingsSaveRequest request,
            HttpServletRequest servletRequest) {
        validateCommonSettingsBoundary(request);
        return ApiResponse.ok(service.saveCommonSettings(request, currentUser(servletRequest).userId()));
    }

    @GetMapping("/api/admin/system-settings/base-years")
    public ApiResponse<List<BaseYearSetting>> listBaseYearSettings() {
        return ApiResponse.ok(service.listBaseYearSettings());
    }

    @PutMapping("/api/admin/system-settings/base-year-current")
    public ApiResponse<BaseYearSetting> saveBaseYearSettings(@Valid @RequestBody BaseYearSaveRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveBaseYearSettings(request, currentUser(servletRequest).userId()));
    }

    @PostMapping("/api/admin/system-settings/base-years/{baseYear}/standards-preparation")
    public ApiResponse<StandardPreparationHistory> prepareBaseYearStandards(@PathVariable Integer baseYear,
            @Valid @RequestBody StandardPreparationRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.prepareBaseYearStandards(baseYear, request, currentUser(servletRequest).userId()));
    }

    @GetMapping("/api/system-settings/default-search-year")
    public ApiResponse<DefaultSearchYearResponse> getDefaultSearchYear() {
        return ApiResponse.ok(service.getDefaultSearchYear());
    }

    private void validateCommonSettingsBoundary(CommonSettingsSaveRequest request) {
        List<ValidationError> fields = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (CommonSettingInput input : request.settings()) {
            String key = input.getSettingKey() == null ? null : input.getSettingKey().trim().toUpperCase();
            if (key != null && !seen.add(key)) {
                fields.add(new ValidationError("settingKey", "같은 setting_key를 중복 저장할 수 없습니다."));
            }
            input.getUnexpectedFields().forEach((field, ignored) -> fields.add(new ValidationError(field, "공통 환경설정에서 허용하지 않는 필드입니다.")));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("공통 환경설정 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
