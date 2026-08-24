package kr.ac.knue.commonfoundation.periodpermissions;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.permissionops.PermissionChangeHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PeriodPermissionService {
    private final PeriodPermissionMapper mapper;
    private final PermissionChangeHistoryMapper historyMapper;
    private final Clock clock;

    @Autowired
    public PeriodPermissionService(PeriodPermissionMapper mapper, PermissionChangeHistoryMapper historyMapper) {
        this(mapper, historyMapper, Clock.systemDefaultZone());
    }

    PeriodPermissionService(PeriodPermissionMapper mapper, PermissionChangeHistoryMapper historyMapper, Clock clock) {
        this.mapper = mapper;
        this.historyMapper = historyMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PeriodPermissionSearchResponse listPeriodPermissions(PeriodPermissionSearchCriteria criteria) {
        return new PeriodPermissionSearchResponse(
                mapper.listPeriodPermissions(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countPeriodPermissions(criteria));
    }

    @Transactional
    public PeriodPermissionRow savePeriodPermission(PeriodPermissionSaveRequest request, Long adminUserId) {
        List<ValidationError> fields = validateSave(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("기간별 권한 저장 요청이 올바르지 않습니다.", fields);
        }
        String periodState = deriveState(request.effectiveStartAt(), request.effectiveEndAt());
        if ("AFTER".equals(periodState)) {
            throw new BusinessValidationException("기간별 권한 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("effectiveEndAt", "서버 처리 시점에 종료된 업무기간의 등록·수정·삭제 요청은 차단됩니다.")));
        }
        PeriodPermissionRow before = mapper.findByKey(request.businessPeriodId(), request.functionPermissionId());
        mapper.upsertPeriodPermission(request.businessPeriodId(), request.functionPermissionId(), request.effectiveStartAt(), request.effectiveEndAt(),
                periodState, adminUserId, request.changeReason());
        PeriodPermissionRow after = mapper.findByKey(request.businessPeriodId(), request.functionPermissionId());
        historyMapper.insertPermissionChangeHistory("FUNCTION", permissionTargetId(request.businessPeriodId(), request.functionPermissionId()),
                toHistoryJson(before), toHistoryJson(after), adminUserId, request.changeReason());
        return after;
    }

    private List<ValidationError> validateSave(PeriodPermissionSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (!hasText(request.businessPeriodId())) {
            fields.add(new ValidationError("businessPeriodId", "업무기간 ID를 입력하세요."));
        }
        if (request.functionPermissionId() == null) {
            fields.add(new ValidationError("functionPermissionId", "기능 권한 ID를 선택하세요."));
        } else if (mapper.existsFunctionPermission(request.functionPermissionId()) == 0) {
            fields.add(new ValidationError("functionPermissionId", "존재하지 않는 기능 권한입니다."));
        }
        if (request.effectiveStartAt() == null) {
            fields.add(new ValidationError("effectiveStartAt", "시작일시를 입력하세요."));
        }
        if (request.effectiveStartAt() != null && request.effectiveEndAt() != null && request.effectiveEndAt().isBefore(request.effectiveStartAt())) {
            fields.add(new ValidationError("effectiveEndAt", "종료일시는 시작일시 이후여야 합니다."));
        }
        return fields;
    }

    private String deriveState(LocalDateTime startAt, LocalDateTime endAt) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(startAt)) {
            return "BEFORE";
        }
        if (endAt != null && now.isAfter(endAt)) {
            return "AFTER";
        }
        return "ACTIVE";
    }

    private String permissionTargetId(String businessPeriodId, Long functionPermissionId) {
        return businessPeriodId + ":" + functionPermissionId;
    }

    private String toHistoryJson(PeriodPermissionRow row) {
        if (row == null) {
            return "null";
        }
        return "{\"businessPeriodId\":\"" + escape(row.businessPeriodId()) + "\",\"functionPermissionId\":\"" + row.functionPermissionId()
                + "\",\"periodState\":\"" + escape(row.periodState()) + "\",\"effectiveAllowed\":\"" + row.effectiveAllowed() + "\"}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
