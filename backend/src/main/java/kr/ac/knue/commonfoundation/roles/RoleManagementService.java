package kr.ac.knue.commonfoundation.roles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleManagementService {
    private static final Set<String> SEED_ROLE_CODES = Set.of("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
    private static final Set<String> IMMUTABLE_FIELDS = Set.of("roleCode", "role_code", "status", "systemUseYn", "system_use_yn");
    private final RoleManagementMapper mapper;

    public RoleManagementService(RoleManagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RoleRow> listRoles(int page, int size, String filter) {
        return mapper.listRoles(new RoleSearchCriteria(page, size, blankToNull(filter)));
    }

    @Transactional
    public RoleRow updateRole(String roleCode, RoleUpdateRequest request, Long currentUserId) {
        validateImmutableAndScope(roleCode, request);
        RoleRow current = mapper.findRoleByCode(roleCode);
        if (current == null) {
            throw validation("R01~R09 기준 역할만 관리할 수 있습니다.", "roleCode", "1차 범위에서는 신규 역할코드 추가가 제외됩니다.");
        }
        List<ValidationError> fields = new ArrayList<>();
        if (!hasText(request.getRoleName())) {
            fields.add(new ValidationError("roleName", "역할명을 입력하세요."));
        }
        if (!hasText(request.getPurpose())) {
            fields.add(new ValidationError("purpose", "역할 목적을 입력하세요."));
        }
        if (!hasText(request.getAssignmentCriteria())) {
            fields.add(new ValidationError("assignmentCriteria", "부여 기준을 입력하세요."));
        }
        if (!hasText(request.getDefaultDataScope())) {
            fields.add(new ValidationError("defaultDataScope", "데이터 범위 기본값을 입력하세요."));
        }
        if (!hasText(request.getChangeReason())) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("역할 기준정보 변경 요청이 올바르지 않습니다.", fields);
        }
        mapper.updateRole(
                roleCode,
                request.getRoleName().trim(),
                request.getPurpose().trim(),
                request.getAssignmentCriteria().trim(),
                request.getDefaultDataScope().trim(),
                currentUserId,
                request.getChangeReason().trim());
        return mapper.findRoleByCode(roleCode);
    }

    private void validateImmutableAndScope(String roleCode, RoleUpdateRequest request) {
        if (!SEED_ROLE_CODES.contains(roleCode)) {
            throw validation("R01~R09 기준 역할만 관리할 수 있습니다.", "roleCode", "1차 범위에서는 신규 역할코드 추가가 제외됩니다.");
        }
        List<ValidationError> immutableErrors = request.getUnexpectedFields().stream()
                .filter(IMMUTABLE_FIELDS::contains)
                .map(field -> new ValidationError(field, "역할코드는 불변이며 시스템 사용여부/상태는 이 화면에서 직접 수정하지 않습니다."))
                .toList();
        if (!immutableErrors.isEmpty()) {
            throw new BusinessValidationException("역할코드는 변경할 수 없습니다.", immutableErrors);
        }
    }

    private BusinessValidationException validation(String message, String field, String fieldMessage) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, fieldMessage)));
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
