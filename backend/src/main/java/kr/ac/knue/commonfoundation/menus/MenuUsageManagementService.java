package kr.ac.knue.commonfoundation.menus;

import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuUsageManagementService {
    private final MenuUsageMapper mapper;

    public MenuUsageManagementService(MenuUsageMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public MenuUsageSearchResponse listMenuUsageSettings(MenuUsageSearchCriteria criteria) {
        return new MenuUsageSearchResponse(
                mapper.listMenuUsageSettings(criteria),
                criteria.safePage(),
                criteria.safeSize(),
                mapper.countMenuUsageSettings(criteria));
    }

    @Transactional
    public List<MenuUsageRow> saveMenuUsageSettings(MenuUsageSettingsRequest request, Long adminUserId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("메뉴 사용 설정 저장 요청이 올바르지 않습니다.", fields);
        }
        List<MenuUsageRow> savedRows = new ArrayList<>();
        for (MenuUsageSettingsRequest.Item item : request.items()) {
            mapper.upsertMenuUsageSetting(item.menuId(), item.systemUseYn(), item.exposureStartAt(),
                    item.exposureEndAt(), adminUserId, item.changeReason());
            savedRows.add(mapper.findMenuUsageSetting(item.menuId()));
        }
        return savedRows;
    }

    private List<ValidationError> validate(MenuUsageSettingsRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || request.items() == null || request.items().isEmpty()) {
            fields.add(new ValidationError("items", "저장할 메뉴 사용 설정을 선택하세요."));
            return fields;
        }
        for (int index = 0; index < request.items().size(); index++) {
            MenuUsageSettingsRequest.Item item = request.items().get(index);
            String prefix = "items[" + index + "].";
            if (item.menuId() == null) {
                fields.add(new ValidationError(prefix + "menuId", "메뉴를 선택하세요."));
            } else if (mapper.existsMenu(item.menuId()) == 0) {
                fields.add(new ValidationError(prefix + "menuId", "존재하지 않는 메뉴입니다."));
            }
            if (!"Y".equals(item.systemUseYn()) && !"N".equals(item.systemUseYn())) {
                fields.add(new ValidationError(prefix + "systemUseYn", "사용여부는 Y 또는 N만 선택할 수 있습니다."));
            }
            if (item.exposureStartAt() == null) {
                fields.add(new ValidationError(prefix + "exposureStartAt", "노출 시작일시를 입력하세요."));
            }
            if (item.exposureEndAt() == null) {
                fields.add(new ValidationError(prefix + "exposureEndAt", "노출 종료일시 정책이 확정되지 않아 값을 입력해야 합니다."));
            } else if (item.exposureStartAt() != null && item.exposureEndAt().isBefore(item.exposureStartAt())) {
                fields.add(new ValidationError(prefix + "exposureEndAt", "노출 종료일시는 시작일시보다 빠를 수 없습니다."));
            }
            if (item.changeReason() == null || item.changeReason().isBlank()) {
                fields.add(new ValidationError(prefix + "changeReason", "변경 사유를 입력하세요."));
            }
        }
        return fields;
    }
}
