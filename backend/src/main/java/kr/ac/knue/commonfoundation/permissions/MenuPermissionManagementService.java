package kr.ac.knue.commonfoundation.permissions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuPermissionManagementService {
    private static final Set<String> TARGET_TYPES = Set.of("ROLE", "ORGANIZATION", "USER");
    private static final Set<String> ACCESS_VALUES = Set.of("ALLOW", "DENY");
    private final MenuPermissionMapper mapper;

    public MenuPermissionManagementService(MenuPermissionMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public MenuPermissionSearchResponse listMenuPermissions(MenuPermissionSearchCriteria criteria) {
        validateSearch(criteria);
        return new MenuPermissionSearchResponse(
                mapper.listMenuPermissions(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countMenuPermissions(criteria));
    }

    @Transactional
    public MenuPermissionRow saveMenuPermission(MenuPermissionSaveRequest request, Long adminUserId) {
        List<ValidationError> fields = validateSave(request);
        throwIfInvalid(fields);
        mapper.upsertPermission(request.targetType(), request.targetId(), request.menuId(), request.accessAllowed(), adminUserId, request.changeReason());
        return mapper.findPermission(request.targetType(), request.targetId(), request.menuId());
    }

    private void validateSearch(MenuPermissionSearchCriteria criteria) {
        if (hasText(criteria.targetType()) && !TARGET_TYPES.contains(criteria.targetType())) {
            throw new BusinessValidationException("메뉴 권한 조회 조건이 올바르지 않습니다.", List.of(new ValidationError("targetType", "ROLE, ORGANIZATION, USER 중 하나를 입력하세요.")));
        }
    }

    private List<ValidationError> validateSave(MenuPermissionSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (!TARGET_TYPES.contains(request.targetType())) {
            fields.add(new ValidationError("targetType", "ROLE, ORGANIZATION, USER 중 하나를 입력하세요."));
        }
        if (!ACCESS_VALUES.contains(request.accessAllowed())) {
            fields.add(new ValidationError("accessAllowed", "ALLOW 또는 DENY만 입력할 수 있습니다."));
        }
        if (mapper.existsMenu(request.menuId()) == 0) {
            fields.add(new ValidationError("menuId", "존재하지 않는 메뉴입니다."));
        }
        if (TARGET_TYPES.contains(request.targetType()) && mapper.existsTarget(request.targetType(), request.targetId()) == 0) {
            fields.add(new ValidationError("targetId", "존재하지 않는 권한 대상입니다."));
        }
        return fields;
    }

    private void throwIfInvalid(List<ValidationError> fields) {
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("메뉴 접근권한 대상 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
