package kr.ac.knue.commonfoundation.basic34;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalIndexingInfoService {
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final Set<String> INDEXING_TYPES = Set.of("KCI", "CANDIDATE", "INTERNATIONAL", "OTHER");
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "journal_indexing_infos";
    private final JournalIndexingInfoMapper mapper;

    public JournalIndexingInfoService(JournalIndexingInfoMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public JournalIndexingInfoSearchResponse list(JournalIndexingInfoSearchCriteria criteria) {
        return new JournalIndexingInfoSearchResponse(
                mapper.listJournalIndexingInfos(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countJournalIndexingInfos(criteria));
    }

    @Transactional
    public JournalIndexingInfoRow save(SaveJournalIndexingInfoRequest request, Long userId, String requestId) {
        SaveJournalIndexingInfoRequest normalized = normalizeAndValidate(request);
        String versionStatus = mapper.findRuleVersionStatus(normalized.ruleVersionId());
        if (versionStatus == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(versionStatus)) {
            throw new ConflictException("확정 또는 폐기된 규정버전의 학술지 등재정보는 수정할 수 없습니다.");
        }
        if (mapper.countOverlappingIssnPeriods(normalized) > 0) {
            throw new ConflictException("같은 ISSN과 유효기간이 중복되는 등재정보를 등록할 수 없습니다.");
        }

        JournalIndexingInfoRow before = mapper.findByKey(normalized);
        mapper.upsertJournalIndexingInfo(normalized, userId);
        JournalIndexingInfoRow after = mapper.findByKey(normalized);
        recordChangeHistory(before, after, normalized, userId, requestId);
        return after;
    }

    private SaveJournalIndexingInfoRequest normalizeAndValidate(SaveJournalIndexingInfoRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.ruleVersionId() == null) fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        if (!hasText(request.issn())) fields.add(new ValidationError("issn", "ISSN을 입력하세요."));
        if (!hasText(request.journalName())) fields.add(new ValidationError("journalName", "학술지명을 입력하세요."));
        if (!hasText(request.indexingType())) {
            fields.add(new ValidationError("indexingType", "등재구분을 선택하세요."));
        } else if (!INDEXING_TYPES.contains(normalized(request.indexingType()))) {
            fields.add(new ValidationError("indexingType", "등재지, 후보지, 국제등재, 기타 중 하나를 선택하세요."));
        }
        if (!hasText(request.publicationCountry())) fields.add(new ValidationError("publicationCountry", "발행국가를 입력하세요."));
        if (request.validStartDate() == null) fields.add(new ValidationError("validStartDate", "유효시작일을 입력하세요."));
        if (request.validEndDate() == null) fields.add(new ValidationError("validEndDate", "유효종료일을 입력하세요."));
        if (request.validStartDate() != null && request.validEndDate() != null
                && request.validEndDate().isBefore(request.validStartDate())) {
            fields.add(new ValidationError("validEndDate", "유효종료일은 시작일 이후여야 합니다."));
        }
        if (!hasText(request.sourceName())) fields.add(new ValidationError("sourceName", "출처를 입력하세요."));
        if (request.sourceUpdatedAt() == null) fields.add(new ValidationError("sourceUpdatedAt", "갱신일시를 입력하세요."));
        if (!USE_FLAGS.contains(trim(request.activeYn()))) fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        if (!hasText(request.changeReason())) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("학술지·후보지 등재정보 저장 요청이 올바르지 않습니다.", fields);
        }
        return new SaveJournalIndexingInfoRequest(
                request.ruleVersionId(),
                request.issn().trim(),
                request.journalName().trim(),
                normalized(request.indexingType()),
                request.publicationCountry().trim(),
                request.validStartDate(),
                request.validEndDate(),
                request.sourceName().trim(),
                request.sourceUpdatedAt(),
                request.activeYn().trim(),
                request.changeReason().trim());
    }

    private void recordChangeHistory(JournalIndexingInfoRow before, JournalIndexingInfoRow after, SaveJournalIndexingInfoRequest request,
                                     Long userId, String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.ruleVersionId() + ":" + request.issn() + ":" + request.validStartDate() + ":" + request.validEndDate();
        recordIfChanged(before == null ? null : before.journalName(), after.journalName(), "journal_name", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.indexingType(), after.indexingType(), "indexing_type", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.publicationCountry(), after.publicationCountry(), "publication_country", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.sourceName(), after.sourceName(), "source_name", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.sourceUpdatedAt(), after.sourceUpdatedAt(), "source_updated_at", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.activeYn(), after.activeYn(), "active_yn", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.changeReason(), after.changeReason(), "change_reason", changeType, targetKey, userId, request.changeReason(), requestId);
    }

    private void recordIfChanged(Object beforeValue, Object afterValue, String fieldName, String changeType,
                                 String targetKey, Long userId, String changeReason, String requestId) {
        String beforeString = beforeValue == null ? null : beforeValue.toString();
        String afterString = afterValue == null ? null : afterValue.toString();
        if (!Objects.equals(beforeString, afterString)) {
            mapper.insertChangeHistory(TARGET_BUSINESS, targetKey, changeType, fieldName, beforeString, afterString,
                    userId, changeReason, requestId);
        }
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String normalized(String value) { return value == null ? null : value.trim().toUpperCase(); }
}
