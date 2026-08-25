package kr.ac.knue.commonfoundation.privacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivacyAccessPermissionService {
    private static final Set<String> ROLE_CODES = Set.of("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
    private static final Set<String> YES_NO = Set.of("Y", "N");
    private static final Set<String> ACCESS_TYPES = Set.of("RAW_VIEW", "MASKED_VIEW", "EXPORT", "ACCOUNT_VIEW");

    private final PrivacyAccessPermissionMapper mapper;

    public PrivacyAccessPermissionService(PrivacyAccessPermissionMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PrivacyAccessPermissionSearchResponse listPrivacyAccessPermissions(PrivacyAccessPermissionSearchCriteria criteria) {
        PrivacyAccessPermissionSearchCriteria normalized = new PrivacyAccessPermissionSearchCriteria(
                Math.max(criteria.page(), 0), criteria.safeSize(), trimToNull(criteria.roleCode()), trimToNull(criteria.fieldKey()));
        return new PrivacyAccessPermissionSearchResponse(
                mapper.listPrivacyAccessPermissions(normalized),
                normalized.page(),
                normalized.safeSize(),
                mapper.countPrivacyAccessPermissions(normalized));
    }

    @Transactional
    public List<PrivacyAccessPermissionRow> savePrivacyAccessPermissions(List<PrivacyAccessPermissionSaveRequest> requests, Long adminUserId) {
        List<ValidationError> fields = validateSaveRequests(requests);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("개인정보 조회권한 저장 요청이 올바르지 않습니다. 사용자 역할 부여·회수는 이 기능 범위가 아닙니다.", fields);
        }
        List<PrivacyAccessPermissionRow> saved = new ArrayList<>();
        for (PrivacyAccessPermissionSaveRequest request : requests) {
            String roleCode = request.roleCode().trim();
            String fieldKey = request.fieldKey().trim();
            mapper.upsertPrivacyAccessPermission(
                    roleCode,
                    fieldKey,
                    request.rawViewAllowedYn(),
                    request.maskedViewAllowedYn(),
                    request.exportAllowedYn(),
                    request.accountViewAllowedYn(),
                    request.changeReason().trim(),
                    adminUserId);
            saved.add(mapper.findByRoleCodeAndFieldKey(roleCode, fieldKey));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public PrivacyAccessEvaluateResponse evaluatePrivacyAccessPermission(PrivacyAccessEvaluateRequest request) {
        List<ValidationError> fields = validateEvaluateRequest(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("개인정보 권한 판정 요청이 올바르지 않습니다.", fields);
        }
        String roleCode = request.roleCode().trim();
        String fieldKey = request.fieldKey().trim();
        String accessType = request.accessType().trim();
        PrivacyAccessPermissionRow permission = mapper.findByRoleCodeAndFieldKey(roleCode, fieldKey);
        if (permission == null) {
            return new PrivacyAccessEvaluateResponse(roleCode, fieldKey, accessType, false, "미설정 역할·필드 조합은 기본 차단됩니다.", false);
        }
        boolean allowed = switch (accessType) {
            case "RAW_VIEW" -> "Y".equals(permission.rawViewAllowedYn());
            case "MASKED_VIEW" -> "Y".equals(permission.maskedViewAllowedYn());
            case "EXPORT" -> "Y".equals(permission.exportAllowedYn());
            case "ACCOUNT_VIEW" -> "Y".equals(permission.accountViewAllowedYn());
            default -> false;
        };
        String reason = allowed ? "권한 설정으로 허용됩니다." : "권한 설정으로 차단됩니다.";
        return new PrivacyAccessEvaluateResponse(roleCode, fieldKey, accessType, allowed, reason, false);
    }

    private List<ValidationError> validateSaveRequests(List<PrivacyAccessPermissionSaveRequest> requests) {
        List<ValidationError> fields = new ArrayList<>();
        if (requests == null || requests.isEmpty()) {
            fields.add(new ValidationError("permissions", "저장할 개인정보 조회권한을 선택하세요."));
            return fields;
        }
        for (int i = 0; i < requests.size(); i++) {
            PrivacyAccessPermissionSaveRequest request = requests.get(i);
            String prefix = requests.size() == 1 ? "" : "permissions[" + i + "].";
            if (request == null) {
                fields.add(new ValidationError(prefix + "permission", "권한 항목이 비어 있습니다."));
                continue;
            }
            validateRoleAndField(request.roleCode(), request.fieldKey(), prefix, fields);
            validateYesNo(request.rawViewAllowedYn(), prefix + "rawViewAllowedYn", "원문 조회 권한", fields);
            validateYesNo(request.maskedViewAllowedYn(), prefix + "maskedViewAllowedYn", "마스킹 조회 권한", fields);
            validateYesNo(request.exportAllowedYn(), prefix + "exportAllowedYn", "출력 권한", fields);
            validateYesNo(request.accountViewAllowedYn(), prefix + "accountViewAllowedYn", "계좌정보 조회 권한", fields);
            if (!hasText(request.changeReason())) {
                fields.add(new ValidationError(prefix + "changeReason", "변경 사유를 입력하세요."));
            }
            if (request.userId() != null) {
                fields.add(new ValidationError(prefix + "userId", "사용자 역할 부여·회수 payload는 개인정보 조회권한 저장 범위가 아닙니다."));
            }
        }
        return fields;
    }

    private List<ValidationError> validateEvaluateRequest(PrivacyAccessEvaluateRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("request", "판정 요청이 비어 있습니다."));
            return fields;
        }
        validateRoleAndField(request.roleCode(), request.fieldKey(), "", fields);
        if (!ACCESS_TYPES.contains(request.accessType())) {
            fields.add(new ValidationError("accessType", "RAW_VIEW, MASKED_VIEW, EXPORT, ACCOUNT_VIEW 중 하나를 선택하세요."));
        }
        if (!hasText(request.processPurpose())) {
            fields.add(new ValidationError("processPurpose", "처리 목적을 입력하세요."));
        }
        return fields;
    }

    private void validateRoleAndField(String roleCode, String fieldKey, String prefix, List<ValidationError> fields) {
        if (!ROLE_CODES.contains(roleCode)) {
            fields.add(new ValidationError(prefix + "roleCode", "R01~R09 기존 역할코드만 사용할 수 있습니다."));
        }
        if (!hasText(fieldKey)) {
            fields.add(new ValidationError(prefix + "fieldKey", "개인정보 필드를 입력하세요."));
        }
    }

    private void validateYesNo(String value, String field, String label, List<ValidationError> fields) {
        if (!YES_NO.contains(value)) {
            fields.add(new ValidationError(field, label + "은 Y 또는 N만 입력할 수 있습니다."));
        }
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
