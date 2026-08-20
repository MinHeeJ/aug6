package kr.ac.knue.commonfoundation.userroles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleManagementService {
    private static final Set<String> ROLE_CODES = Set.of("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
    private final UserRoleManagementMapper mapper;

    public UserRoleManagementService(UserRoleManagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public UserRoleAssignmentSearchResponse listAssignments(UserRoleAssignmentSearchCriteria criteria) {
        return new UserRoleAssignmentSearchResponse(mapper.listAssignments(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countAssignments(criteria));
    }

    @Transactional(readOnly = true)
    public UserRoleAssignmentSearchResponse listCurrentUserRoles(Long userId, int page, int size) {
        int safeSize = size < 1 ? 20 : Math.min(size, 100);
        int offset = Math.max(page, 0) * safeSize;
        requireExistingUser(userId);
        return new UserRoleAssignmentSearchResponse(mapper.listCurrentUserRoles(userId, safeSize, offset), Math.max(page, 0), safeSize, mapper.countCurrentUserRoles(userId));
    }

    @Transactional
    public UserRoleAssignmentSummary assign(UserRoleAssignmentRequest request, Long adminUserId) {
        ValidatedAssignment validated = validateAssignmentRequest(null, request, true);
        mapper.insertAssignment(validated.userId(), validated.roleCode(), validated.assignmentType(), validated.validStartDate(), request.getValidEndDate(), adminUserId, request.getChangeReason());
        UserRoleAssignmentSummary created = mapper.findAssignmentByDetails(
                validated.userId(), validated.roleCode(), validated.assignmentType(),
                validated.validStartDate(), request.getValidEndDate());
        if (created == null) {
            throw new IllegalArgumentException("사용자 역할 부여 결과를 찾을 수 없습니다.");
        }
        return created;
    }

    @Transactional
    public UserRoleAssignmentSummary update(Long assignmentId, UserRoleAssignmentRequest request, Long adminUserId) {
        UserRoleAssignmentSummary current = requireExistingAssignment(assignmentId);
        if ("POSITION".equals(current.assignmentType())) {
            throw new BusinessValidationException("보직 기반 역할은 사용자 역할 관리에서 직접 변경할 수 없습니다.", List.of(new ValidationError("assignmentType", "POSITION 역할은 보직 기준으로만 관리됩니다.")));
        }
        ValidatedAssignment validated = validateAssignmentRequest(assignmentId, request, false);
        mapper.updateAssignment(assignmentId, validated.userId(), validated.roleCode(), validated.assignmentType(), validated.validStartDate(), request.getValidEndDate(), adminUserId, request.getChangeReason());
        return requireExistingAssignment(assignmentId);
    }

    @Transactional
    public UserRoleAssignmentSummary revoke(Long assignmentId, RevokeUserRoleRequest request, Long adminUserId) {
        UserRoleAssignmentSummary current = requireExistingAssignment(assignmentId);
        if ("POSITION".equals(current.assignmentType())) {
            throw new BusinessValidationException("보직 기반 역할은 사용자 역할 관리에서 직접 회수할 수 없습니다.", List.of(new ValidationError("assignmentType", "POSITION 역할은 보직 기준으로만 관리됩니다.")));
        }
        if (request.getChangeReason() != null && request.getChangeReason().length() > 500) {
            throw new BusinessValidationException("사용자 역할 회수 요청이 올바르지 않습니다.", List.of(new ValidationError("changeReason", "변경 사유는 500자 이하여야 합니다.")));
        }
        mapper.revokeAssignment(assignmentId, adminUserId, request.getChangeReason());
        return requireExistingAssignment(assignmentId);
    }

    private ValidatedAssignment validateAssignmentRequest(Long assignmentId, UserRoleAssignmentRequest request, boolean create) {
        List<ValidationError> fields = new ArrayList<>();
        Long userId = parseUserId(request.getUserId(), fields);
        String roleCode = trim(request.getRoleCode());
        String assignmentType = trim(request.getAssignmentType());
        LocalDate start = request.getValidStartDate() == null ? LocalDate.now() : request.getValidStartDate();
        if (userId != null && mapper.existsUser(userId) == 0) {
            fields.add(new ValidationError("userId", "존재하지 않는 사용자입니다."));
        }
        if (!ROLE_CODES.contains(roleCode) || mapper.existsActiveRole(roleCode) == 0) {
            fields.add(new ValidationError("roleCode", "R01~R09의 활성 역할만 선택할 수 있습니다."));
        }
        if (!Set.of("MANUAL", "POSITION").contains(assignmentType)) {
            fields.add(new ValidationError("assignmentType", "MANUAL 또는 POSITION만 입력할 수 있습니다."));
        } else if ("POSITION".equals(assignmentType)) {
            fields.add(new ValidationError("assignmentType", "POSITION 역할은 보직 기준으로만 관리됩니다."));
        }
        if (request.getValidEndDate() != null && request.getValidEndDate().isBefore(start)) {
            fields.add(new ValidationError("validEndDate", "역할 유효 종료일은 시작일보다 빠를 수 없습니다."));
        }
        if (request.getChangeReason() != null && request.getChangeReason().length() > 500) {
            fields.add(new ValidationError("changeReason", "변경 사유는 500자 이하여야 합니다."));
        }
        if (fields.isEmpty() && create && mapper.countOverlappingActiveAssignment(null, userId, roleCode, start, request.getValidEndDate()) > 0) {
            fields.add(new ValidationError("roleCode", "동일 기간에 이미 활성 사용자 역할이 존재합니다."));
        }
        if (fields.isEmpty() && !create && mapper.countOverlappingActiveAssignment(assignmentId, userId, roleCode, start, request.getValidEndDate()) > 0) {
            fields.add(new ValidationError("roleCode", "동일 기간에 이미 활성 사용자 역할이 존재합니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("사용자 역할 요청이 올바르지 않습니다.", fields);
        }
        return new ValidatedAssignment(userId, roleCode, assignmentType, start);
    }

    private Long parseUserId(String value, List<ValidationError> fields) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            fields.add(new ValidationError("userId", "사용자 식별자는 숫자여야 합니다."));
            return null;
        }
    }

    private void requireExistingUser(Long userId) {
        if (userId == null || mapper.existsUser(userId) == 0) {
            throw new BusinessValidationException("사용자를 찾을 수 없습니다.", List.of(new ValidationError("userId", "존재하지 않는 사용자입니다.")));
        }
    }

    private UserRoleAssignmentSummary requireExistingAssignment(Long assignmentId) {
        UserRoleAssignmentSummary row = mapper.findAssignmentById(assignmentId);
        if (row == null) {
            throw new BusinessValidationException("사용자 역할 부여 정보를 찾을 수 없습니다.", List.of(new ValidationError("assignmentId", "존재하지 않는 사용자 역할입니다.")));
        }
        return row;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private record ValidatedAssignment(Long userId, String roleCode, String assignmentType, LocalDate validStartDate) {
    }
}
