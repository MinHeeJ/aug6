package kr.ac.knue.commonfoundation.permissionops;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionChangeHistoryService {
    private static final Set<String> TARGET_TYPES = Set.of("ROLE", "MENU", "FUNCTION", "DATA_SCOPE", "TEMPORARY");
    private final PermissionChangeHistoryMapper mapper;

    public PermissionChangeHistoryService(PermissionChangeHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PermissionChangeHistorySearchResponse listPermissionChangeHistory(PermissionChangeHistorySearchCriteria criteria) {
        List<ValidationError> fields = validate(criteria);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("권한 변경 이력 검색조건이 올바르지 않습니다.", fields);
        }
        PermissionChangeHistorySearchCriteria normalized = criteria.normalized();
        return new PermissionChangeHistorySearchResponse(
                mapper.listPermissionChangeHistory(normalized),
                normalized.page(),
                normalized.size(),
                mapper.countPermissionChangeHistory(normalized));
    }

    private List<ValidationError> validate(PermissionChangeHistorySearchCriteria criteria) {
        List<ValidationError> fields = new ArrayList<>();
        if (criteria.targetType() != null && !criteria.targetType().isBlank() && !TARGET_TYPES.contains(criteria.targetType())) {
            fields.add(new ValidationError("targetType", "ROLE, MENU, FUNCTION, DATA_SCOPE, TEMPORARY 중 하나를 선택하세요."));
        }
        if (criteria.size() < 1 || criteria.size() > 100) {
            fields.add(new ValidationError("size", "페이지 크기는 1~100 사이여야 합니다."));
        }
        if (criteria.page() < 0) {
            fields.add(new ValidationError("page", "페이지 번호는 0 이상이어야 합니다."));
        }
        return fields;
    }
}
