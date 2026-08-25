package kr.ac.knue.commonfoundation.privacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivacyAccessLogService {
    private static final Set<String> PROCESS_TYPES = Set.of("VIEW", "PRINT", "DOWNLOAD");
    private static final Set<String> PROCESS_RESULTS = Set.of("SUCCESS", "DENIED", "FAILED");
    private final PrivacyAccessLogMapper mapper;

    public PrivacyAccessLogService(PrivacyAccessLogMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PrivacyAccessLogSearchResponse searchPrivacyAccessLogs(PrivacyAccessLogSearchCriteria criteria) {
        PrivacyAccessLogSearchCriteria normalized = new PrivacyAccessLogSearchCriteria(
                Math.max(criteria.page(), 0), criteria.safeSize(), criteria.actorUserId(), blankToNull(criteria.targetRef()),
                blankToNull(criteria.processType()), blankToNull(criteria.processedFrom()), blankToNull(criteria.processedTo()));
        return new PrivacyAccessLogSearchResponse(
                mapper.searchPrivacyAccessLogs(normalized),
                normalized.page(),
                normalized.safeSize(),
                mapper.countPrivacyAccessLogs(normalized));
    }

    @Transactional(readOnly = true)
    public PrivacyAccessLogRow getPrivacyAccessLog(Long historyId) {
        PrivacyAccessLogRow row = mapper.findPrivacyAccessLog(historyId);
        if (row == null) {
            throw new NotFoundException("개인정보 처리이력을 찾을 수 없습니다.");
        }
        return row;
    }

    @Transactional
    public PrivacyAccessLogRow recordPrivacyAccessLog(PrivacyAccessLogRecordRequest request, String requestIp) {
        List<ValidationError> fields = validate(request, requestIp);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("개인정보 처리 목적과 처리이력 기록 요청을 확인하세요.", fields);
        }
        mapper.insertPrivacyAccessLog(
                request.processType(),
                request.actorUserId(),
                request.targetRef().trim(),
                request.processPurpose().trim(),
                requestIp.trim(),
                request.processResult());
        return mapper.findLatestForActorTargetPurpose(request.actorUserId(), request.targetRef().trim(), request.processPurpose().trim());
    }

    private List<ValidationError> validate(PrivacyAccessLogRecordRequest request, String requestIp) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("request", "처리이력 기록 요청이 비어 있습니다."));
            return fields;
        }
        if (!PROCESS_TYPES.contains(request.processType())) {
            fields.add(new ValidationError("processType", "VIEW, PRINT, DOWNLOAD 중 하나를 선택하세요."));
        }
        if (request.actorUserId() == null) {
            fields.add(new ValidationError("actorUserId", "처리자를 입력하세요."));
        }
        if (!hasText(request.targetRef())) {
            fields.add(new ValidationError("targetRef", "대상자 참조를 입력하세요."));
        }
        if (!hasText(request.processPurpose())) {
            fields.add(new ValidationError("processPurpose", "처리 목적을 입력하세요."));
        }
        if (!PROCESS_RESULTS.contains(request.processResult())) {
            fields.add(new ValidationError("processResult", "SUCCESS, DENIED, FAILED 중 하나를 선택하세요."));
        }
        if (!hasText(requestIp)) {
            fields.add(new ValidationError("requestIp", "요청 IP를 확인할 수 없습니다."));
        }
        if (hasText(request.actualValue())) {
            fields.add(new ValidationError("actualValue", "개인정보 원문값은 처리이력에 기록할 수 없습니다."));
        }
        if (hasText(request.originalValue())) {
            fields.add(new ValidationError("originalValue", "개인정보 원문값은 처리이력에 기록할 수 없습니다."));
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
