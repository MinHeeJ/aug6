package kr.ac.knue.commonfoundation.functionpermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.permissionops.PermissionChangeHistoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FunctionPermissionService {
    private static final Set<String> FUNCTION_TYPES = Set.of("READ", "CREATE", "UPDATE", "DELETE", "EXECUTE");
    private static final Set<String> PERMISSION_VALUES = Set.of("ALLOW", "DENY");
    private static final Set<String> MUTATING_FUNCTIONS = Set.of("CREATE", "UPDATE", "DELETE", "EXECUTE");
    private final FunctionPermissionMapper mapper;
    private final PermissionChangeHistoryMapper historyMapper;

    public FunctionPermissionService(FunctionPermissionMapper mapper, PermissionChangeHistoryMapper historyMapper) {
        this.mapper = mapper;
        this.historyMapper = historyMapper;
    }

    @Transactional(readOnly = true)
    public FunctionPermissionSearchResponse listFunctionPermissions(FunctionPermissionSearchCriteria criteria) {
        return new FunctionPermissionSearchResponse(
                mapper.listFunctionPermissions(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countFunctionPermissions(criteria));
    }

    @Transactional
    public FunctionPermissionRow saveFunctionPermission(FunctionPermissionSaveRequest request, Long adminUserId) {
        List<ValidationError> fields = validateSave(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("기능 권한 저장 요청이 올바르지 않습니다.", fields);
        }
        FunctionPermissionRow before = mapper.findByKey(request.screenId(), request.roleCode(), request.functionType());
        mapper.upsertFunctionPermission(request.screenId(), request.roleCode(), request.functionType(), request.permissionAllowed(), adminUserId, request.changeReason());
        FunctionPermissionRow after = mapper.findByKey(request.screenId(), request.roleCode(), request.functionType());
        historyMapper.insertPermissionChangeHistory("FUNCTION", permissionTargetId(request.screenId(), request.roleCode(), request.functionType()), toHistoryJson(before), toHistoryJson(after), adminUserId, request.changeReason());
        return after;
    }

    @Transactional(readOnly = true)
    public FunctionPermissionEvaluateResponse evaluate(FunctionPermissionEvaluateRequest request) {
        List<ValidationError> fields = validateEvaluate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("기능 권한 판정 요청이 올바르지 않습니다.", fields);
        }
        if ("EVALUATION_CONFIRMED".equals(request.targetDataStatus()) && MUTATING_FUNCTIONS.contains(request.functionType())) {
            throw new ForbiddenException();
        }
        FunctionPermissionRow row = mapper.findByKey(request.screenId(), request.roleCode(), request.functionType());
        if (row != null && "ALLOW".equals(row.permissionAllowed())) {
            return new FunctionPermissionEvaluateResponse(true, request.screenId(), request.roleCode(), request.functionType(), "ALLOW");
        }
        throw new ForbiddenException();
    }

    private List<ValidationError> validateSave(FunctionPermissionSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        validateCommon(request.screenId(), request.roleCode(), request.functionType(), fields);
        if (!PERMISSION_VALUES.contains(request.permissionAllowed())) {
            fields.add(new ValidationError("permissionAllowed", "ALLOW 또는 DENY만 입력할 수 있습니다."));
        }
        return fields;
    }

    private List<ValidationError> validateEvaluate(FunctionPermissionEvaluateRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        validateCommon(request.screenId(), request.roleCode(), request.functionType(), fields);
        if (!hasText(request.targetDataStatus())) {
            fields.add(new ValidationError("targetDataStatus", "대상 데이터 상태를 입력하세요."));
        }
        return fields;
    }

    private void validateCommon(String screenId, String roleCode, String functionType, List<ValidationError> fields) {
        if (!hasText(screenId)) {
            fields.add(new ValidationError("screenId", "화면 ID를 입력하세요."));
        } else if (mapper.existsScreen(screenId) == 0) {
            fields.add(new ValidationError("screenId", "존재하지 않는 화면입니다."));
        }
        if (!hasText(roleCode)) {
            fields.add(new ValidationError("roleCode", "역할 코드를 선택하세요."));
        } else if (mapper.existsRole(roleCode) == 0) {
            fields.add(new ValidationError("roleCode", "존재하지 않는 역할입니다."));
        }
        if (!FUNCTION_TYPES.contains(functionType)) {
            fields.add(new ValidationError("functionType", "READ, CREATE, UPDATE, DELETE, EXECUTE 중 하나를 선택하세요."));
        }
    }

    private String permissionTargetId(String screenId, String roleCode, String functionType) {
        return screenId + ":" + roleCode + ":" + functionType;
    }

    private String toHistoryJson(FunctionPermissionRow row) {
        if (row == null) {
            return "null";
        }
        return "{\"screenId\":\"" + escape(row.screenId()) + "\",\"roleCode\":\"" + escape(row.roleCode())
                + "\",\"functionType\":\"" + escape(row.functionType()) + "\",\"permissionAllowed\":\""
                + escape(row.permissionAllowed()) + "\"}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
