package kr.ac.knue.commonfoundation.privacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivacyFieldPolicyService {
    private static final Set<String> PRIVACY_GRADES = Set.of("PUBLIC", "PERSONAL", "SENSITIVE", "ACCOUNT");
    private static final Set<String> YES_NO = Set.of("Y", "N");
    private final PrivacyFieldPolicyMapper mapper;

    public PrivacyFieldPolicyService(PrivacyFieldPolicyMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PrivacyFieldPolicySearchResponse listPrivacyFieldPolicies(PrivacyFieldPolicySearchCriteria criteria) {
        PrivacyFieldPolicySearchCriteria normalized = new PrivacyFieldPolicySearchCriteria(
                Math.max(criteria.page(), 0), criteria.safeSize(), criteria.fieldKey(), criteria.privacyGrade(), criteria.encryptionRequiredYn());
        return new PrivacyFieldPolicySearchResponse(
                mapper.listPrivacyFieldPolicies(normalized),
                normalized.page(),
                normalized.safeSize(),
                mapper.countPrivacyFieldPolicies(normalized));
    }

    @Transactional
    public List<PrivacyFieldPolicyRow> savePrivacyFieldPolicies(List<PrivacyFieldPolicySaveRequest> requests, Long adminUserId) {
        List<ValidationError> fields = validateRequests(requests);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("개인정보 보호정책 저장 요청이 올바르지 않습니다. 원문값은 정책 API에 포함할 수 없습니다.", fields);
        }
        List<PrivacyFieldPolicyRow> saved = new ArrayList<>();
        for (PrivacyFieldPolicySaveRequest request : requests) {
            mapper.upsertPrivacyFieldPolicy(
                    request.fieldKey().trim(),
                    request.privacyGrade(),
                    request.encryptionRequiredYn(),
                    blankToNull(request.maskingRule()),
                    request.logExclusionYn(),
                    request.changeReason().trim(),
                    adminUserId);
            saved.add(mapper.findByFieldKey(request.fieldKey().trim()));
        }
        return saved;
    }

    private List<ValidationError> validateRequests(List<PrivacyFieldPolicySaveRequest> requests) {
        List<ValidationError> fields = new ArrayList<>();
        if (requests == null || requests.isEmpty()) {
            fields.add(new ValidationError("policies", "저장할 개인정보 보호정책을 선택하세요."));
            return fields;
        }
        for (int i = 0; i < requests.size(); i++) {
            PrivacyFieldPolicySaveRequest request = requests.get(i);
            String prefix = requests.size() == 1 ? "" : "policies[" + i + "].";
            if (request == null) {
                fields.add(new ValidationError(prefix + "policy", "정책 항목이 비어 있습니다."));
                continue;
            }
            if (!hasText(request.fieldKey())) {
                fields.add(new ValidationError(prefix + "fieldKey", "개인정보 필드를 입력하세요."));
            }
            if (!PRIVACY_GRADES.contains(request.privacyGrade())) {
                fields.add(new ValidationError(prefix + "privacyGrade", "PUBLIC, PERSONAL, SENSITIVE, ACCOUNT 중 하나를 선택하세요."));
            }
            if (!YES_NO.contains(request.encryptionRequiredYn())) {
                fields.add(new ValidationError(prefix + "encryptionRequiredYn", "Y 또는 N만 입력할 수 있습니다."));
            }
            if (!YES_NO.contains(request.logExclusionYn())) {
                fields.add(new ValidationError(prefix + "logExclusionYn", "Y 또는 N만 입력할 수 있습니다."));
            }
            if (!hasText(request.changeReason())) {
                fields.add(new ValidationError(prefix + "changeReason", "변경 사유를 입력하세요."));
            }
            if (hasText(request.actualValue())) {
                fields.add(new ValidationError(prefix + "actualValue", "실제 개인정보 원문값은 보호정책 payload에 포함할 수 없습니다."));
            }
            if (hasText(request.originalValue())) {
                fields.add(new ValidationError(prefix + "originalValue", "실제 개인정보 원문값은 보호정책 payload에 포함할 수 없습니다."));
            }
        }
        return fields;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
