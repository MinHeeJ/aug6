package kr.ac.knue.commonfoundation.securitysessions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActiveSessionService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private final ActiveSessionMapper mapper;

    public ActiveSessionService(ActiveSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ActiveSessionSearchResponse listActiveSessions(int page, int size, ActiveSessionSearchCriteria criteria) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        ActiveSessionSearchCriteria normalized = new ActiveSessionSearchCriteria(
                blankToNull(criteria == null ? null : criteria.filter()), "ACTIVE");
        return new ActiveSessionSearchResponse(
                mapper.listActiveSessions(normalized, safeSize, safePage * safeSize),
                safePage, safeSize, mapper.countActiveSessions(normalized));
    }

    @Transactional
    public ActiveSessionRow terminateActiveSession(String sessionId, TerminateActiveSessionRequest request,
            Long operatorUserId, String requestId) {
        validateTermination(sessionId, request);
        String normalizedSessionId = sessionId.trim();
        ActiveSessionRow current = mapper.findSessionForUpdate(normalizedSessionId);
        if (current == null) {
            throw new NotFoundException("강제종료 대상 세션을 찾을 수 없습니다.");
        }
        if (!"ACTIVE".equals(current.status())) {
            throw new ConflictException("ACTIVE 상태의 세션만 강제종료할 수 있습니다.");
        }
        String reason = request.getReason().trim();
        String effectiveRequestId = effectiveRequestId(requestId);
        mapper.markTerminated(normalizedSessionId, reason, operatorUserId, effectiveRequestId);
        mapper.insertTerminationHistory(normalizedSessionId, reason);
        mapper.insertSessionTerminateAudit(normalizedSessionId, operatorUserId, reason, effectiveRequestId);
        return mapper.findSessionForUpdate(normalizedSessionId);
    }

    @Transactional(readOnly = true)
    public SessionTerminationHistorySearchResponse listSessionTerminationHistories(int page, int size,
            SessionTerminationHistorySearchCriteria criteria) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        SessionTerminationHistorySearchCriteria normalized = normalizeHistoryCriteria(criteria);
        return new SessionTerminationHistorySearchResponse(
                mapper.listSessionTerminationHistories(normalized, safeSize, safePage * safeSize),
                safePage, safeSize, mapper.countSessionTerminationHistories(normalized));
    }

    private SessionTerminationHistorySearchCriteria normalizeHistoryCriteria(SessionTerminationHistorySearchCriteria criteria) {
        if (criteria == null) {
            return new SessionTerminationHistorySearchCriteria(null, null, null, null);
        }
        List<ValidationError> fields = new ArrayList<>();
        String terminationType = blankToNull(criteria.terminationType());
        if (terminationType != null
                && !Set.of("LOGOUT", "IDLE_TIMEOUT", "ABSOLUTE_TIMEOUT", "ADMIN_TERMINATED").contains(terminationType)) {
            fields.add(new ValidationError("terminationType", "종료유형은 LOGOUT, IDLE_TIMEOUT, ABSOLUTE_TIMEOUT, ADMIN_TERMINATED 중 하나여야 합니다."));
        }
        if (criteria.fromDate() != null && criteria.toDate() != null && criteria.fromDate().isAfter(criteria.toDate())) {
            fields.add(new ValidationError("fromDate", "기간 시작일은 종료일보다 늦을 수 없습니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("세션 종료 이력 조회 조건이 올바르지 않습니다.", fields);
        }
        return new SessionTerminationHistorySearchCriteria(blankToNull(criteria.filter()), terminationType,
                criteria.fromDate(), criteria.toDate());
    }

    private void validateTermination(String sessionId, TerminateActiveSessionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (blankToNull(sessionId) == null) {
            fields.add(new ValidationError("sessionId", "세션을 선택하세요."));
        }
        if (request == null) {
            fields.add(new ValidationError("body", "강제종료 요청 본문이 필요합니다."));
            throw new BusinessValidationException("세션 강제종료 요청이 올바르지 않습니다.", fields);
        }
        request.getUnexpectedFields().forEach(field -> fields.add(new ValidationError(field, "접속현황 화면에서 허용하지 않는 필드입니다.")));
        if (blankToNull(request.getReason()) == null) {
            fields.add(new ValidationError("reason", "강제종료 사유를 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("세션 강제종료 요청이 올바르지 않습니다.", fields);
        }
    }

    private String effectiveRequestId(String requestId) {
        return blankToNull(requestId) == null ? UUID.randomUUID().toString() : requestId.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
