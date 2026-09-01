package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KorusFacultySyncService {
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private final KorusFacultySyncMapper mapper;

    public KorusFacultySyncService(KorusFacultySyncMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public KorusFacultySyncSearchResponse list(KorusFacultySyncSearchCriteria criteria) {
        return new KorusFacultySyncSearchResponse(mapper.listResults(criteria), Math.max(criteria.page(), 0),
                criteria.safeSize(), mapper.countResults(criteria));
    }

    @Transactional
    public KorusFacultySyncRunRow createRun(KorusFacultySyncRunRequest request, Long createdBy, String requestId) {
        validatePeriod(request);
        String effectiveRequestId = effectiveRequestId(requestId);
        if (Boolean.TRUE.equals(mapper.requestIdExists(effectiveRequestId))) {
            throw new ConflictException("이미 처리된 KORUS 교원 동기화 요청입니다.");
        }
        mapper.insertRun(effectiveRequestId, "MANUAL", request.targetStartDate(), request.targetEndDate(), createdBy);
        KorusFacultySyncRunRow run = mapper.findRunByRequestId(effectiveRequestId);
        List<KorusFacultySourceRow> sources = mapper.listFacultySources(request.targetStartDate(), request.targetEndDate());
        int successCount = 0;
        int failureCount = 0;
        for (KorusFacultySourceRow source : sources) {
            boolean organizationExists = Boolean.TRUE.equals(mapper.organizationExists(source.organizationCode()));
            String status = organizationExists ? SUCCESS : FAILED;
            String errorMessage = organizationExists ? null : "조직 매핑 실패";
            mapper.upsertResult(run.runId(), effectiveRequestId, source, status, errorMessage, null);
            if (organizationExists) successCount++; else failureCount++;
        }
        String runStatus = failureCount == 0 ? SUCCESS : successCount == 0 ? FAILED : "PARTIAL";
        String failureReason = failureCount == 0 ? null : "일부 KORUS 교원 기본정보의 조직 매핑에 실패했습니다.";
        mapper.updateRunCounts(run.runId(), runStatus, sources.size(), successCount, failureCount, failureReason);
        return mapper.findRunByRequestId(effectiveRequestId);
    }

    @Transactional
    public KorusFacultySyncRunRow retryFailedResult(Long resultId, Long createdBy, String requestId) {
        if (resultId == null) {
            throw new BusinessValidationException("재처리 대상이 올바르지 않습니다.",
                    List.of(new ValidationError("resultId", "재처리할 실패 건을 선택하세요.")));
        }
        KorusFacultySyncResultRow original = mapper.findResult(resultId);
        if (original == null) {
            throw new NotFoundException("KORUS 교원 동기화 결과를 찾을 수 없습니다.");
        }
        if (!FAILED.equals(original.syncStatus())) {
            throw new BusinessValidationException("실패 건만 재처리할 수 있습니다.",
                    List.of(new ValidationError("resultId", "성공 건은 재처리 대상이 아닙니다.")));
        }
        String effectiveRequestId = effectiveRequestId(requestId);
        if (Boolean.TRUE.equals(mapper.requestIdExists(effectiveRequestId))) {
            throw new ConflictException("이미 처리된 KORUS 교원 동기화 재처리 요청입니다.");
        }
        LocalDate today = LocalDate.now();
        mapper.insertRun(effectiveRequestId, "RETRY", today, today, createdBy);
        KorusFacultySyncRunRow run = mapper.findRunByRequestId(effectiveRequestId);
        KorusFacultySourceRow source = new KorusFacultySourceRow(original.employeeNo(), original.name(),
                original.organizationCode(), original.rankName(), original.appointmentId());
        boolean organizationExists = Boolean.TRUE.equals(mapper.organizationExists(source.organizationCode()));
        String status = organizationExists ? SUCCESS : FAILED;
        mapper.upsertResult(run.runId(), effectiveRequestId, source, status,
                organizationExists ? null : "조직 매핑 실패", original.resultId());
        mapper.updateRunCounts(run.runId(), status, 1, organizationExists ? 1 : 0, organizationExists ? 0 : 1,
                organizationExists ? null : "KORUS 교원 기본정보 재처리에 실패했습니다.");
        return mapper.findRunByRequestId(effectiveRequestId);
    }

    private void validatePeriod(KorusFacultySyncRunRequest request) {
        if (request.targetStartDate() != null && request.targetEndDate() != null
                && request.targetEndDate().isBefore(request.targetStartDate())) {
            throw new BusinessValidationException("KORUS 교원 동기화 대상기간이 올바르지 않습니다.",
                    List.of(new ValidationError("targetEndDate", "대상기간 종료일은 시작일 이후여야 합니다.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) return requestId.trim();
        return UUID.randomUUID().toString();
    }
}
