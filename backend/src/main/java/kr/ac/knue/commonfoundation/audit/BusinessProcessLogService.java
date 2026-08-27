package kr.ac.knue.commonfoundation.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessProcessLogService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private static final Set<String> ACTION_TYPES = Set.of("CREATE", "UPDATE", "DELETE", "CONFIRM", "AUTH", "APPROVE", "CANCEL", "BATCH", "SESSION_TERMINATE");
    private static final Set<String> RESULT_STATUSES = Set.of("SUCCESS", "FAILURE");

    private final BusinessProcessLogMapper mapper;

    public BusinessProcessLogService(BusinessProcessLogMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public BusinessProcessLogSearchResponse listBusinessProcessLogs(int page, int size, BusinessProcessLogSearchCriteria criteria) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        BusinessProcessLogSearchCriteria normalized = normalizeCriteria(criteria);
        return new BusinessProcessLogSearchResponse(
                mapper.listBusinessProcessLogs(normalized, safeSize, safePage * safeSize),
                safePage, safeSize, mapper.countBusinessProcessLogs(normalized));
    }

    private BusinessProcessLogSearchCriteria normalizeCriteria(BusinessProcessLogSearchCriteria criteria) {
        if (criteria == null) {
            return new BusinessProcessLogSearchCriteria(null, null, null, null, null, null, null);
        }
        List<ValidationError> fields = new ArrayList<>();
        String actionType = blankToNull(criteria.actionType());
        if (actionType != null && !ACTION_TYPES.contains(actionType)) {
            fields.add(new ValidationError("actionType", "행위유형은 CREATE, UPDATE, DELETE, CONFIRM, AUTH, APPROVE, CANCEL, BATCH, SESSION_TERMINATE 중 하나여야 합니다."));
        }
        String resultStatus = blankToNull(criteria.resultStatus());
        if (resultStatus != null && !RESULT_STATUSES.contains(resultStatus)) {
            fields.add(new ValidationError("resultStatus", "처리결과는 SUCCESS 또는 FAILURE 중 하나여야 합니다."));
        }
        if (criteria.fromDate() != null && criteria.toDate() != null && criteria.fromDate().isAfter(criteria.toDate())) {
            fields.add(new ValidationError("fromDate", "기간 시작일은 종료일보다 늦을 수 없습니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("업무처리 로그 조회 조건이 올바르지 않습니다.", fields);
        }
        return new BusinessProcessLogSearchCriteria(blankToNull(criteria.filter()), actionType,
                blankToNull(criteria.targetKey()), criteria.actorUserId(), resultStatus, criteria.fromDate(), criteria.toDate());
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
