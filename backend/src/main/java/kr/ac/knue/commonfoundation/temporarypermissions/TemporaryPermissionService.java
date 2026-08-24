package kr.ac.knue.commonfoundation.temporarypermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.permissionops.PermissionChangeHistoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemporaryPermissionService {
    private static final Set<String> FUNCTION_TYPES = Set.of("READ", "CREATE", "UPDATE", "DELETE", "EXECUTE");
    private final TemporaryPermissionMapper mapper;
    private final PermissionChangeHistoryMapper historyMapper;

    public TemporaryPermissionService(TemporaryPermissionMapper mapper, PermissionChangeHistoryMapper historyMapper) {
        this.mapper = mapper;
        this.historyMapper = historyMapper;
    }

    @Transactional
    public TemporaryPermissionSearchResponse listTemporaryPermissions(TemporaryPermissionSearchCriteria criteria) {
        mapper.expireElapsedTemporaryPermissions();
        return new TemporaryPermissionSearchResponse(
                mapper.listTemporaryPermissions(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countTemporaryPermissions(criteria));
    }

    @Transactional
    public TemporaryPermissionRow createTemporaryPermission(TemporaryPermissionCreateRequest request, Long adminUserId) {
        List<ValidationError> fields = validateCreate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("임시 권한 저장 요청이 올바르지 않습니다.", fields);
        }
        mapper.insertTemporaryPermission(request.userId(), request.workDataRef(), request.functionType(), request.validStartAt(), request.validEndAt(), request.changeReason(), adminUserId);
        TemporaryPermissionRow after = mapper.findById(mapper.lastInsertedId());
        historyMapper.insertPermissionChangeHistory("TEMPORARY", String.valueOf(after.temporaryPermissionId()), "null", toHistoryJson(after), adminUserId, request.changeReason());
        return after;
    }

    private List<ValidationError> validateCreate(TemporaryPermissionCreateRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.userId() == null) {
            fields.add(new ValidationError("userId", "대상 교원을 선택하세요."));
        } else if (mapper.existsUser(request.userId()) == 0) {
            fields.add(new ValidationError("userId", "존재하지 않는 사용자입니다."));
        }
        if (!hasText(request.workDataRef())) {
            fields.add(new ValidationError("workDataRef", "업무자료 식별자를 입력하세요."));
        }
        if (!FUNCTION_TYPES.contains(request.functionType())) {
            fields.add(new ValidationError("functionType", "READ, CREATE, UPDATE, DELETE, EXECUTE 중 하나를 선택하세요."));
        }
        if (request.validStartAt() == null) {
            fields.add(new ValidationError("validStartAt", "유효 시작일시를 입력하세요."));
        }
        if (request.validEndAt() == null) {
            fields.add(new ValidationError("validEndAt", "유효 종료일시를 입력하세요."));
        }
        if (request.validStartAt() != null && request.validEndAt() != null && request.validEndAt().isBefore(request.validStartAt())) {
            fields.add(new ValidationError("validEndAt", "유효 종료일시는 시작일시보다 빠를 수 없습니다."));
        }
        return fields;
    }

    private String toHistoryJson(TemporaryPermissionRow row) {
        return "{\"temporaryPermissionId\":" + row.temporaryPermissionId()
                + ",\"userId\":" + row.userId()
                + ",\"workDataRef\":\"" + escape(row.workDataRef())
                + "\",\"functionType\":\"" + escape(row.functionType())
                + "\",\"status\":\"" + escape(row.status()) + "\"}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
