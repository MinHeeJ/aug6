package kr.ac.knue.commonfoundation.basic32;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessStatusCodeService {
    private static final Set<String> BUSINESS_TYPES = Set.of("FACULTY_ACHIEVEMENT", "ACADEMIC_GRANT", "OBJECTION");
    private static final Set<String> DEFINITION_VERSIONS = Set.of("DRAFT", "CONFIRMED", "DISCARDED");
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final String DRAFT = "DRAFT";
    private final BusinessStatusCodeMapper mapper;

    public BusinessStatusCodeService(BusinessStatusCodeMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public BusinessStatusCodeSearchResponse list(BusinessStatusCodeSearchCriteria criteria) {
        return new BusinessStatusCodeSearchResponse(
                mapper.listStatusCodes(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countStatusCodes(criteria));
    }

    @Transactional
    public BusinessStatusCodeRow save(BusinessStatusCodeSaveRequest request, Long adminUserId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("업무 상태코드 저장 요청이 올바르지 않습니다.", fields);
        }
        if (!DRAFT.equals(request.definitionVersion())) {
            throw new ConflictException("확정 또는 폐기된 상태정의 버전의 기술 상태코드는 수정할 수 없습니다.");
        }

        BusinessStatusCodeRow before = mapper.findByKey(request.businessType(), request.definitionVersion(), request.statusCode());
        mapper.upsertDraftStatusCode(
                request.definitionVersion(),
                request.businessType(),
                request.statusCode(),
                request.displayName(),
                request.systemUseYn(),
                request.changeReason(),
                adminUserId);
        BusinessStatusCodeRow after = mapper.findByKey(request.businessType(), request.definitionVersion(), request.statusCode());
        recordChangeHistory(before, after, request, adminUserId);
        return after;
    }

    private List<ValidationError> validate(BusinessStatusCodeSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (!DEFINITION_VERSIONS.contains(request.definitionVersion())) {
            fields.add(new ValidationError("definitionVersion", "DRAFT, CONFIRMED, DISCARDED 중 하나를 선택하세요."));
        }
        if (!BUSINESS_TYPES.contains(request.businessType())) {
            fields.add(new ValidationError("businessType", "FACULTY_ACHIEVEMENT, ACADEMIC_GRANT, OBJECTION 중 하나를 선택하세요."));
        }
        if (!hasText(request.statusCode())) {
            fields.add(new ValidationError("statusCode", "상태코드를 입력하세요."));
        }
        if (!hasText(request.displayName())) {
            fields.add(new ValidationError("displayName", "상태 표시명을 입력하세요."));
        }
        if (!USE_FLAGS.contains(request.systemUseYn())) {
            fields.add(new ValidationError("systemUseYn", "Y 또는 N을 선택하세요."));
        }
        if (!hasText(request.changeReason())) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        return fields;
    }

    private void recordChangeHistory(
            BusinessStatusCodeRow before,
            BusinessStatusCodeRow after,
            BusinessStatusCodeSaveRequest request,
            Long adminUserId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.businessType() + ":" + request.statusCode();
        if (before == null || !Objects.equals(before.displayName(), after.displayName())) {
            mapper.insertChangeHistory(
                    "business_status_codes",
                    targetKey,
                    changeType,
                    "display_name",
                    before == null ? null : before.displayName(),
                    after.displayName(),
                    adminUserId,
                    request.changeReason());
        }
        if (before != null && !Objects.equals(before.systemUseYn(), after.systemUseYn())) {
            mapper.insertChangeHistory(
                    "business_status_codes",
                    targetKey,
                    changeType,
                    "system_use_yn",
                    before.systemUseYn(),
                    after.systemUseYn(),
                    adminUserId,
                    request.changeReason());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
