package kr.ac.knue.commonfoundation.users;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {
    private static final Set<String> KORUS_SOURCE_FIELDS = Set.of(
            "employeeNo", "name", "organizationCode", "organizationCodeFilter", "rankName", "employmentStatus", "positionName", "retirementDate", "lastSyncedAt"
    );
    private final UserManagementMapper mapper;

    public UserManagementService(UserManagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public UserSearchResponse search(UserSearchCriteria criteria) {
        List<UserSummary> rows = mapper.searchUsers(criteria).stream()
                .map(row -> new UserSummary(row.userId(), row.loginId(), row.employeeNo(), row.name(), row.organizationCode(), row.organizationName(),
                        row.rankName(), row.employmentStatus(), row.positionName(), row.retirementDate(), row.lastSyncedAt(), row.systemUseYn(), row.status(), mapper.findRoleCodesByUserId(row.userId())))
                .toList();
        return new UserSearchResponse(rows, mapper.findAvailableRoles(), criteria.page(), criteria.safeSize(), mapper.countUsers(criteria));
    }

    @Transactional
    public UserSummary updateAccount(Long userId, UpdateUserAccountRequest request, Long adminUserId) {
        requireExistingUser(userId);
        rejectKorusSourceMutation(request.getUnexpectedFields());
        List<ValidationError> fields = new ArrayList<>();
        if (!Set.of("Y", "N").contains(request.getSystemUseYn())) {
            fields.add(new ValidationError("systemUseYn", "Y 또는 N만 입력할 수 있습니다."));
        }
        if (hasText(request.getChangeReason()) && request.getChangeReason().length() > 500) {
            fields.add(new ValidationError("changeReason", "변경 사유는 500자 이하여야 합니다."));
        }
        throwIfInvalid("사용자 사용여부 변경 요청이 올바르지 않습니다.", fields);
        mapper.updateUserAccount(userId, request.getSystemUseYn(), request.getChangeReason(), adminUserId);
        return findUpdatedUser(userId);
    }

    @Transactional
    public List<UserRoleSummary> updateRoles(Long userId, UpdateUserRolesRequest request, Long adminUserId) {
        requireExistingUser(userId);
        rejectKorusSourceMutation(request.getUnexpectedFields());
        List<ValidationError> fields = new ArrayList<>();
        LocalDate start = request.getValidStartDate() == null ? LocalDate.now() : request.getValidStartDate();
        if (request.getValidEndDate() != null && request.getValidEndDate().isBefore(start)) {
            fields.add(new ValidationError("validEndDate", "역할 유효 종료일은 시작일보다 빠를 수 없습니다."));
        }
        List<String> normalizedRoleCodes = request.getRoleCodes() == null ? List.of() : request.getRoleCodes().stream()
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedRoleCodes.isEmpty()) {
            fields.add(new ValidationError("roleCodes", "업무 역할을 하나 이상 선택하세요."));
        } else if (mapper.countActiveRoleCodes(normalizedRoleCodes) != normalizedRoleCodes.size()) {
            fields.add(new ValidationError("roleCodes", "R01~R09의 활성 역할만 선택할 수 있습니다."));
        }
        if (hasText(request.getChangeReason()) && request.getChangeReason().length() > 500) {
            fields.add(new ValidationError("changeReason", "변경 사유는 500자 이하여야 합니다."));
        }
        throwIfInvalid("사용자 업무 역할 변경 요청이 올바르지 않습니다.", fields);
        mapper.endManualRoles(userId, request.getChangeReason());
        for (String roleCode : normalizedRoleCodes) {
            mapper.insertManualRole(userId, roleCode, start, request.getValidEndDate(), adminUserId, request.getChangeReason());
        }
        return mapper.findCurrentRolesByUserId(userId);
    }

    private UserSummary findUpdatedUser(Long userId) {
        UserSummary row = mapper.findUserById(userId);
        if (row == null) {
            throw new BusinessValidationException("사용자를 찾을 수 없습니다.", List.of(new ValidationError("userId", "존재하지 않는 사용자입니다.")));
        }
        return new UserSummary(row.userId(), row.loginId(), row.employeeNo(), row.name(), row.organizationCode(), row.organizationName(),
                row.rankName(), row.employmentStatus(), row.positionName(), row.retirementDate(), row.lastSyncedAt(), row.systemUseYn(), row.status(), mapper.findRoleCodesByUserId(row.userId()));
    }

    private void requireExistingUser(Long userId) {
        if (userId == null || mapper.existsUser(userId) == 0) {
            throw new BusinessValidationException("사용자를 찾을 수 없습니다.", List.of(new ValidationError("userId", "존재하지 않는 사용자입니다.")));
        }
    }

    private void rejectKorusSourceMutation(Set<String> unexpectedFields) {
        Set<String> korusFields = new LinkedHashSet<>(unexpectedFields);
        korusFields.retainAll(KORUS_SOURCE_FIELDS);
        if (!korusFields.isEmpty()) {
            throw new BusinessValidationException("KORUS 원천 인사정보는 사용자 관리에서 직접 수정할 수 없습니다.",
                    korusFields.stream().map(field -> new ValidationError(field, "KORUS 원천정보는 읽기 전용입니다.")).toList());
        }
    }

    private void throwIfInvalid(String message, List<ValidationError> fields) {
        if (!fields.isEmpty()) {
            throw new BusinessValidationException(message, fields);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
