package kr.ac.knue.commonfoundation.basic32;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RejectionReasonService {
    private static final Set<String> BUSINESS_TYPES = Set.of("FACULTY_ACHIEVEMENT", "ACADEMIC_GRANT", "OBJECTION");
    private static final Set<String> FLAGS = Set.of("Y", "N");
    private final RejectionReasonMapper mapper;

    public RejectionReasonService(RejectionReasonMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public RejectionReasonSearchResponse list(RejectionReasonSearchCriteria criteria) {
        return new RejectionReasonSearchResponse(
                mapper.listReasons(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countReasons(criteria));
    }

    @Transactional
    public RejectionReasonRow save(RejectionReasonSaveRequest request, Long adminUserId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("반려사유 저장 요청이 올바르지 않습니다.", fields);
        }

        RejectionReasonRow before = mapper.findByKey(request.businessType(), request.reasonCode());
        mapper.upsertRejectionReason(
                request.businessType(),
                request.reasonCode(),
                request.standardMessage(),
                request.additionalOpinionAllowedYn(),
                request.changeReason(),
                adminUserId);
        RejectionReasonRow after = mapper.findByKey(request.businessType(), request.reasonCode());
        recordChangeHistory(before, after, request, adminUserId);
        return after;
    }

    private List<ValidationError> validate(RejectionReasonSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (!hasText(request.businessType()) || !BUSINESS_TYPES.contains(request.businessType())) {
            fields.add(new ValidationError("businessType", "FACULTY_ACHIEVEMENT, ACADEMIC_GRANT, OBJECTION 중 하나를 선택하세요."));
        }
        if (!hasText(request.reasonCode())) {
            fields.add(new ValidationError("reasonCode", "반려사유 코드를 입력하세요."));
        }
        if (!hasText(request.standardMessage())) {
            fields.add(new ValidationError("standardMessage", "표준 문구를 입력하세요."));
        }
        if (!hasText(request.additionalOpinionAllowedYn()) || !FLAGS.contains(request.additionalOpinionAllowedYn())) {
            fields.add(new ValidationError("additionalOpinionAllowedYn", "Y 또는 N을 선택하세요."));
        }
        if (!hasText(request.changeReason())) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        return fields;
    }

    private void recordChangeHistory(
            RejectionReasonRow before,
            RejectionReasonRow after,
            RejectionReasonSaveRequest request,
            Long adminUserId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.businessType() + ":" + request.reasonCode();
        recordIfChanged(before == null ? null : before.standardMessage(), after.standardMessage(), "standard_message", changeType, targetKey, adminUserId, request.changeReason());
        recordIfChanged(before == null ? null : before.additionalOpinionAllowedYn(), after.additionalOpinionAllowedYn(), "additional_opinion_allowed_yn", changeType, targetKey, adminUserId, request.changeReason());
    }

    private void recordIfChanged(String beforeValue, String afterValue, String fieldName, String changeType, String targetKey, Long adminUserId, String changeReason) {
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory("rejection_reasons", targetKey, changeType, fieldName, beforeValue, afterValue, adminUserId, changeReason);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
