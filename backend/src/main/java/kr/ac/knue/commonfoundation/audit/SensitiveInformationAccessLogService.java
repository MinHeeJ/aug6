package kr.ac.knue.commonfoundation.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SensitiveInformationAccessLogService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private static final Set<String> INFORMATION_TYPES = Set.of(
            "PERSONAL_EVALUATION_RESULT", "SCORE_CALCULATION", "PERSONAL_INFORMATION", "ACCOUNT_INFORMATION");
    private static final Set<String> ACCESS_RESULTS = Set.of("SUCCESS", "FAILURE");

    private final SensitiveInformationAccessLogMapper mapper;

    public SensitiveInformationAccessLogService(SensitiveInformationAccessLogMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public SensitiveInformationAccessLogSearchResponse listSensitiveInformationAccessLogs(
            int page, int size, SensitiveInformationAccessLogSearchCriteria criteria) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        SensitiveInformationAccessLogSearchCriteria normalized = normalizeCriteria(criteria);
        return new SensitiveInformationAccessLogSearchResponse(
                mapper.listSensitiveInformationAccessLogs(normalized, safeSize, safePage * safeSize),
                safePage, safeSize, mapper.countSensitiveInformationAccessLogs(normalized));
    }

    private SensitiveInformationAccessLogSearchCriteria normalizeCriteria(SensitiveInformationAccessLogSearchCriteria criteria) {
        if (criteria == null) {
            return new SensitiveInformationAccessLogSearchCriteria(null, null, null, null, null, null);
        }
        List<ValidationError> fields = new ArrayList<>();
        String informationType = blankToNull(criteria.informationType());
        if (informationType != null && !INFORMATION_TYPES.contains(informationType)) {
            fields.add(new ValidationError("informationType", "정보유형은 PERSONAL_EVALUATION_RESULT, SCORE_CALCULATION, PERSONAL_INFORMATION, ACCOUNT_INFORMATION 중 하나여야 합니다."));
        }
        String accessResult = blankToNull(criteria.accessResult());
        if (accessResult != null && !ACCESS_RESULTS.contains(accessResult)) {
            fields.add(new ValidationError("accessResult", "조회결과는 SUCCESS 또는 FAILURE 중 하나여야 합니다."));
        }
        if (criteria.fromDate() != null && criteria.toDate() != null && criteria.fromDate().isAfter(criteria.toDate())) {
            fields.add(new ValidationError("fromDate", "기간 시작일은 종료일보다 늦을 수 없습니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("중요정보 조회 로그 조회 조건이 올바르지 않습니다.", fields);
        }
        return new SensitiveInformationAccessLogSearchCriteria(blankToNull(criteria.filter()), informationType,
                criteria.viewerUserId(), accessResult, criteria.fromDate(), criteria.toDate());
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
