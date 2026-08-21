package kr.ac.knue.commonfoundation.operations;

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
public class OperationsManagementService {
    private static final Set<String> DATA_SCOPE_TYPES = Set.of("SELF", "DEPARTMENT", "COLLEGE", "DUTY", "ALL");
    private static final Set<String> ROLE_CODES = Set.of("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
    private final OperationsManagementMapper mapper;

    public OperationsManagementService(OperationsManagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PositionAssignmentSearchResponse searchPositionAssignments(AssignmentSearchCriteria criteria) {
        AssignmentSearchCriteria safe = safeCriteria(criteria);
        return new PositionAssignmentSearchResponse(mapper.searchPositionAssignments(safe), Math.max(safe.page(), 0), safe.safeSize(), mapper.countPositionAssignments(safe));
    }

    @Transactional
    public PositionAssignmentRow savePositionAssignment(PositionAssignmentRequest request, Long currentUserId) {
        validatePositionRequest(request);
        Long userId = parseId(request.getUserId(), "userId");
        validateUserAndOrganization(userId, request.getOrganizationCode());
        mapper.insertPositionAssignment(trim(request.getPositionCode()), userId, trim(request.getOrganizationCode()), request.getEffectiveStartDate(), request.getEffectiveEndDate(), currentUserId, trim(request.getChangeReason()));
        AssignmentSearchCriteria criteria = new AssignmentSearchCriteria(0, 20, request.getEffectiveStartDate(), trim(request.getPositionCode()));
        return mapper.searchPositionAssignments(criteria).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("저장된 보직 지정을 조회할 수 없습니다."));
    }

    @Transactional
    public PositionAssignmentRow updatePositionAssignment(Long id, PositionAssignmentRequest request, Long currentUserId) {
        validatePositionRequest(request);
        if (mapper.findPositionAssignmentById(id) == null) {
            throw validation("보직 지정 정보를 찾을 수 없습니다.", "positionAssignmentId", "존재하는 보직 지정을 선택하세요.");
        }
        Long userId = parseId(request.getUserId(), "userId");
        validateUserAndOrganization(userId, request.getOrganizationCode());
        mapper.updatePositionAssignment(id, trim(request.getPositionCode()), userId, trim(request.getOrganizationCode()), request.getEffectiveStartDate(), request.getEffectiveEndDate(), currentUserId, trim(request.getChangeReason()));
        return mapper.findPositionAssignmentById(id);
    }

    @Transactional(readOnly = true)
    public DutyAssignmentSearchResponse searchDutyAssignments(AssignmentSearchCriteria criteria) {
        AssignmentSearchCriteria safe = safeCriteria(criteria);
        return new DutyAssignmentSearchResponse(mapper.searchDutyAssignments(safe), Math.max(safe.page(), 0), safe.safeSize(), mapper.countDutyAssignments(safe));
    }

    @Transactional
    public DutyAssignmentRow saveDutyAssignment(DutyAssignmentRequest request, Long currentUserId) {
        validateDutyRequest(request);
        Long userId = parseId(request.getUserId(), "userId");
        if (mapper.existsUser(userId) == 0) {
            throw validation("담당자를 찾을 수 없습니다.", "userId", "존재하는 사용자를 선택하세요.");
        }
        mapper.insertDutyAssignment(trim(request.getDutyOrganization()), userId, trim(request.getDutyArea()), request.getValidStartDate(), request.getValidEndDate(), trim(request.getDataScopeType()), trim(request.getProcessingPermission()), currentUserId, trim(request.getChangeReason()));
        return mapper.searchDutyAssignments(new AssignmentSearchCriteria(0, 20, request.getValidStartDate(), trim(request.getDutyArea()))).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("저장된 담당자 지정을 조회할 수 없습니다."));
    }

    @Transactional
    public DutyAssignmentRow updateDutyAssignment(Long id, DutyAssignmentRequest request, Long currentUserId) {
        validateDutyRequest(request);
        if (mapper.findDutyAssignmentById(id) == null) {
            throw validation("업무담당자 지정을 찾을 수 없습니다.", "dutyAssignmentId", "존재하는 지정을 선택하세요.");
        }
        Long userId = parseId(request.getUserId(), "userId");
        if (mapper.existsUser(userId) == 0) {
            throw validation("담당자를 찾을 수 없습니다.", "userId", "존재하는 사용자를 선택하세요.");
        }
        mapper.updateDutyAssignment(id, trim(request.getDutyOrganization()), userId, trim(request.getDutyArea()), request.getValidStartDate(), request.getValidEndDate(), trim(request.getDataScopeType()), trim(request.getProcessingPermission()), currentUserId, trim(request.getChangeReason()));
        return mapper.findDutyAssignmentById(id);
    }

    @Transactional(readOnly = true)
    public DataScopeRulesSearchResponse searchDataScopeRules(AssignmentSearchCriteria criteria) {
        AssignmentSearchCriteria safe = safeCriteria(criteria);
        return new DataScopeRulesSearchResponse(mapper.searchDataScopeRules(safe), Math.max(safe.page(), 0), safe.safeSize(), mapper.countDataScopeRules(safe));
    }

    @Transactional
    public DataScopeRuleRow saveDataScopeRules(DataScopeRulesSaveRequest request, Long currentUserId) {
        validateDataScopeRequest(request);
        String roleCode = trim(request.getRoleCode());
        String dataScopeType = trim(request.getDataScopeType());
        String organizationCode = blankToNull(request.getOrganizationCode());
        String dutyArea = blankToNull(request.getDutyArea());
        DataScopeRuleRow existing = mapper.findDataScopeRule(roleCode, dataScopeType, organizationCode, dutyArea);
        if (existing == null) {
            mapper.insertDataScopeRule(roleCode, dataScopeType, organizationCode, dutyArea, currentUserId, blankToNull(request.getChangeReason()));
        } else {
            mapper.updateDataScopeRule(existing.dataScopeRuleId(), dataScopeType, organizationCode, dutyArea, currentUserId, blankToNull(request.getChangeReason()));
        }
        return mapper.findDataScopeRule(roleCode, dataScopeType, organizationCode, dutyArea);
    }

    @Transactional(readOnly = true)
    public Set<String> resolveUnionDataScopes(List<String> roleCodes) {
        LinkedHashSet<String> scopes = new LinkedHashSet<>();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return scopes;
        }
        mapper.findRulesByRoles(roleCodes.stream().filter(ROLE_CODES::contains).toList()).forEach(row -> scopes.add(row.dataScopeType()));
        return scopes;
    }

    private AssignmentSearchCriteria safeCriteria(AssignmentSearchCriteria criteria) {
        return new AssignmentSearchCriteria(criteria.page(), criteria.safeSize(), criteria.referenceDate(), blankToNull(criteria.filter()));
    }

    private void validatePositionRequest(PositionAssignmentRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.getEffectiveStartDate() != null && request.getEffectiveEndDate() != null && request.getEffectiveEndDate().isBefore(request.getEffectiveStartDate())) {
            fields.add(new ValidationError("effectiveEndDate", "종료일은 시작일보다 빠를 수 없습니다."));
        }
        if (!hasText(request.getChangeReason())) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("보직 지정 요청이 올바르지 않습니다.", fields);
    }

    private void validateDutyRequest(DutyAssignmentRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (!DATA_SCOPE_TYPES.contains(trim(request.getDataScopeType()))) fields.add(new ValidationError("dataScopeType", "허용된 데이터 범위 유형을 선택하세요."));
        if (request.getValidStartDate() != null && request.getValidEndDate() != null && request.getValidEndDate().isBefore(request.getValidStartDate())) fields.add(new ValidationError("validEndDate", "종료일은 시작일보다 빠를 수 없습니다."));
        if (!fields.isEmpty()) throw new BusinessValidationException("업무담당자 지정 요청이 올바르지 않습니다.", fields);
    }

    private void validateDataScopeRequest(DataScopeRulesSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String roleCode = trim(request.getRoleCode());
        if (!ROLE_CODES.contains(roleCode)) fields.add(new ValidationError("roleCode", "R01~R09 역할만 선택할 수 있습니다."));
        else if (mapper.existsRole(roleCode) == 0) fields.add(new ValidationError("roleCode", "등록된 역할코드를 선택하세요."));
        if (!DATA_SCOPE_TYPES.contains(trim(request.getDataScopeType()))) fields.add(new ValidationError("dataScopeType", "허용된 데이터 범위 유형을 선택하세요."));
        if ("DUTY".equals(trim(request.getDataScopeType())) && !hasText(request.getDutyArea())) fields.add(new ValidationError("dutyArea", "담당업무 범위에는 업무영역이 필요합니다."));
        if (!fields.isEmpty()) throw new BusinessValidationException("데이터 범위 규칙 요청이 올바르지 않습니다.", fields);
    }

    private void validateUserAndOrganization(Long userId, String organizationCode) {
        if (mapper.existsUser(userId) == 0) throw validation("사용자를 찾을 수 없습니다.", "userId", "존재하는 사용자를 선택하세요.");
        if (mapper.existsOrganization(trim(organizationCode)) == 0) throw validation("조직을 찾을 수 없습니다.", "organizationCode", "존재하는 조직을 선택하세요.");
    }

    private Long parseId(String value, String field) {
        try { return Long.valueOf(value); }
        catch (NumberFormatException ex) { throw validation("식별자 형식이 올바르지 않습니다.", field, "숫자 식별자를 사용하세요."); }
    }

    private BusinessValidationException validation(String message, String field, String fieldMessage) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, fieldMessage)));
    }

    private String trim(String value) { return value == null ? null : value.trim(); }
    private String blankToNull(String value) { return hasText(value) ? value.trim() : null; }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
