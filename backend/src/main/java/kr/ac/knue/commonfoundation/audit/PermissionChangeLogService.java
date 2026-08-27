package kr.ac.knue.commonfoundation.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionChangeLogService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private static final Set<String> TARGET_TYPES = Set.of("ROLE", "MENU", "FUNCTION", "DATA_SCOPE", "TEMPORARY");

    private final PermissionChangeLogMapper mapper;

    public PermissionChangeLogService(PermissionChangeLogMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PermissionChangeLogSearchResponse listPermissionChangeLogs(
            int page, int size, PermissionChangeLogSearchCriteria criteria) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        PermissionChangeLogSearchCriteria normalized = normalizeCriteria(criteria);
        return new PermissionChangeLogSearchResponse(
                mapper.listPermissionChangeLogs(normalized, safeSize, safePage * safeSize),
                safePage, safeSize, mapper.countPermissionChangeLogs(normalized));
    }

    private PermissionChangeLogSearchCriteria normalizeCriteria(PermissionChangeLogSearchCriteria criteria) {
        if (criteria == null) {
            return new PermissionChangeLogSearchCriteria(null, null, null, null, null, null);
        }
        List<ValidationError> fields = new ArrayList<>();
        String targetType = blankToNull(criteria.targetType());
        if (targetType != null && !TARGET_TYPES.contains(targetType)) {
            fields.add(new ValidationError("targetType", "권한유형은 ROLE, MENU, FUNCTION, DATA_SCOPE, TEMPORARY 중 하나여야 합니다."));
        }
        if (criteria.fromDate() != null && criteria.toDate() != null && criteria.fromDate().isAfter(criteria.toDate())) {
            fields.add(new ValidationError("fromDate", "기간 시작일은 종료일보다 늦을 수 없습니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("권한변경 로그 조회 조건이 올바르지 않습니다.", fields);
        }
        return new PermissionChangeLogSearchCriteria(blankToNull(criteria.targetId()), targetType,
                criteria.approverUserId(), criteria.changedBy(), criteria.fromDate(), criteria.toDate());
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
