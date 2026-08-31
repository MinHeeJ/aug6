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
public class BusinessStatusTransitionService {
    private static final Set<String> BUSINESS_TYPES = Set.of("FACULTY_ACHIEVEMENT", "ACADEMIC_GRANT", "OBJECTION");
    private static final Set<String> DEFINITION_VERSIONS = Set.of("DRAFT", "CONFIRMED", "DISCARDED");
    private static final Set<String> FLAGS = Set.of("Y", "N");
    private static final Set<String> ROLES = Set.of("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
    private static final String DRAFT = "DRAFT";
    private final BusinessStatusTransitionMapper mapper;

    public BusinessStatusTransitionService(BusinessStatusTransitionMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public BusinessStatusTransitionSearchResponse list(BusinessStatusTransitionSearchCriteria criteria) {
        return new BusinessStatusTransitionSearchResponse(
                mapper.listTransitions(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countTransitions(criteria));
    }

    @Transactional
    public BusinessStatusTransitionRow save(BusinessStatusTransitionSaveRequest request, Long adminUserId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("업무 상태 전이 저장 요청이 올바르지 않습니다.", fields);
        }
        if (!DRAFT.equals(request.definitionVersion())) {
            throw new ConflictException("확정 또는 폐기된 상태정의 버전의 전이규칙은 수정할 수 없습니다.");
        }
        if (mapper.statusCodeExists(request.businessType(), request.definitionVersion(), request.fromStatusCode()) == 0) {
            throw new BusinessValidationException("현재 상태코드가 작성중 상태정의 버전에 없습니다.", List.of(new ValidationError("fromStatusCode", "등록된 작성중 상태코드를 선택하세요.")));
        }
        if (mapper.statusCodeExists(request.businessType(), request.definitionVersion(), request.toStatusCode()) == 0) {
            throw new BusinessValidationException("다음 상태코드가 작성중 상태정의 버전에 없습니다.", List.of(new ValidationError("toStatusCode", "등록된 작성중 상태코드를 선택하세요.")));
        }
        if (mapper.roleExists(request.executorRoleCode()) == 0) {
            throw new BusinessValidationException("기존 R01~R09 역할코드만 실행 역할로 선택할 수 있습니다.", List.of(new ValidationError("executorRoleCode", "기존 역할코드를 선택하세요.")));
        }

        BusinessStatusTransitionRow before = mapper.findByKey(
                request.businessType(), request.definitionVersion(), request.fromStatusCode(), request.toStatusCode(), request.executorRoleCode());
        mapper.upsertDraftTransition(
                request.definitionVersion(),
                request.businessType(),
                request.fromStatusCode(),
                request.toStatusCode(),
                request.executorRoleCode(),
                request.opinionRequiredYn(),
                request.attachmentRequiredYn(),
                request.cancellableYn(),
                request.changeReason(),
                adminUserId);
        BusinessStatusTransitionRow after = mapper.findByKey(
                request.businessType(), request.definitionVersion(), request.fromStatusCode(), request.toStatusCode(), request.executorRoleCode());
        recordChangeHistory(before, after, request, adminUserId);
        return after;
    }

    private List<ValidationError> validate(BusinessStatusTransitionSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (!DEFINITION_VERSIONS.contains(request.definitionVersion())) {
            fields.add(new ValidationError("definitionVersion", "DRAFT, CONFIRMED, DISCARDED 중 하나를 선택하세요."));
        }
        if (!BUSINESS_TYPES.contains(request.businessType())) {
            fields.add(new ValidationError("businessType", "FACULTY_ACHIEVEMENT, ACADEMIC_GRANT, OBJECTION 중 하나를 선택하세요."));
        }
        if (!hasText(request.fromStatusCode())) {
            fields.add(new ValidationError("fromStatusCode", "현재 상태코드를 입력하세요."));
        }
        if (!hasText(request.toStatusCode())) {
            fields.add(new ValidationError("toStatusCode", "다음 상태코드를 입력하세요."));
        }
        if (!ROLES.contains(request.executorRoleCode())) {
            fields.add(new ValidationError("executorRoleCode", "R01~R09 중 하나를 선택하세요."));
        }
        if (!FLAGS.contains(request.opinionRequiredYn())) {
            fields.add(new ValidationError("opinionRequiredYn", "Y 또는 N을 선택하세요."));
        }
        if (!FLAGS.contains(request.attachmentRequiredYn())) {
            fields.add(new ValidationError("attachmentRequiredYn", "Y 또는 N을 선택하세요."));
        }
        if (!FLAGS.contains(request.cancellableYn())) {
            fields.add(new ValidationError("cancellableYn", "Y 또는 N을 선택하세요."));
        }
        if (Objects.equals(request.fromStatusCode(), request.toStatusCode())) {
            fields.add(new ValidationError("toStatusCode", "현재 상태와 다른 다음 상태를 선택하세요."));
        }
        if (!hasText(request.changeReason())) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        return fields;
    }

    private void recordChangeHistory(
            BusinessStatusTransitionRow before,
            BusinessStatusTransitionRow after,
            BusinessStatusTransitionSaveRequest request,
            Long adminUserId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.businessType() + ":" + request.fromStatusCode() + "->" + request.toStatusCode() + ":" + request.executorRoleCode();
        recordIfChanged(before == null ? null : before.opinionRequiredYn(), after.opinionRequiredYn(), "opinion_required_yn", changeType, targetKey, adminUserId, request.changeReason());
        recordIfChanged(before == null ? null : before.attachmentRequiredYn(), after.attachmentRequiredYn(), "attachment_required_yn", changeType, targetKey, adminUserId, request.changeReason());
        recordIfChanged(before == null ? null : before.cancellableYn(), after.cancellableYn(), "cancellable_yn", changeType, targetKey, adminUserId, request.changeReason());
    }

    private void recordIfChanged(String beforeValue, String afterValue, String fieldName, String changeType, String targetKey, Long adminUserId, String changeReason) {
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory("business_status_transitions", targetKey, changeType, fieldName, beforeValue, afterValue, adminUserId, changeReason);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
