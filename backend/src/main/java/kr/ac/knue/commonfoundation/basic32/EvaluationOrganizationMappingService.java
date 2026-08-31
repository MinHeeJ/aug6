package kr.ac.knue.commonfoundation.basic32;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationOrganizationMappingService {
    private static final Set<String> BUSINESS_TYPES = Set.of("FACULTY_ACHIEVEMENT", "ACADEMIC_GRANT", "OBJECTION");
    private static final Set<String> DATA_SCOPES = Set.of("SELF", "DEPARTMENT", "COLLEGE", "BUSINESS", "ALL");
    private final EvaluationOrganizationMappingMapper mapper;

    public EvaluationOrganizationMappingService(EvaluationOrganizationMappingMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationOrganizationMappingSearchResponse list(EvaluationOrganizationMappingSearchCriteria criteria) {
        return new EvaluationOrganizationMappingSearchResponse(
                mapper.listMappings(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countMappings(criteria));
    }

    @Transactional
    public EvaluationOrganizationMappingRow save(EvaluationOrganizationMappingSaveRequest request, Long adminUserId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("평가조직 매핑 저장 요청이 올바르지 않습니다.", fields);
        }
        mapper.upsertMapping(request.userId(), request.organizationCode(), request.businessType(), request.dataScope(), request.changeReason(), adminUserId);
        return mapper.findByKey(request.userId(), request.organizationCode(), request.businessType());
    }

    private List<ValidationError> validate(EvaluationOrganizationMappingSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.userId() == null) {
            fields.add(new ValidationError("userId", "사용자 ID를 입력하세요."));
        } else if (mapper.existsUser(request.userId()) == 0) {
            fields.add(new ValidationError("userId", "존재하지 않는 사용자입니다."));
        }
        if (!hasText(request.organizationCode())) {
            fields.add(new ValidationError("organizationCode", "조직코드를 입력하세요."));
        } else if (mapper.existsOrganization(request.organizationCode()) == 0) {
            fields.add(new ValidationError("organizationCode", "존재하지 않는 조직입니다."));
        }
        if (!BUSINESS_TYPES.contains(request.businessType())) {
            fields.add(new ValidationError("businessType", "FACULTY_ACHIEVEMENT, ACADEMIC_GRANT, OBJECTION 중 하나를 선택하세요."));
        }
        if (!DATA_SCOPES.contains(request.dataScope())) {
            fields.add(new ValidationError("dataScope", "SELF, DEPARTMENT, COLLEGE, BUSINESS, ALL 중 하나를 선택하세요."));
        }
        if (!hasText(request.changeReason())) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        return fields;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
