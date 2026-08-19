package kr.ac.knue.commonfoundation.codes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetailCodeManagementService {
    private static final Set<String> IMMUTABLE_OR_OUT_OF_SCOPE_FIELDS = Set.of("groupId", "status", "createdAt", "createdBy", "updatedAt", "updatedBy");
    private final DetailCodeManagementMapper mapper;

    public DetailCodeManagementService(DetailCodeManagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DetailCodeRow> listDetailCodes(String groupId, int page, int size) {
        return listDetailCodes(groupId, null, page, size);
    }

    @Transactional(readOnly = true)
    public List<DetailCodeRow> listDetailCodes(String groupId, String filter, int page, int size) {
        String normalizedGroupId = normalizeIdentity(groupId);
        ensureCodeGroupExists(normalizedGroupId);
        return mapper.listDetailCodes(new DetailCodeSearchCriteria(normalizedGroupId, filter, page, size));
    }

    @Transactional
    public DetailCodeRow createDetailCode(String groupId, DetailCodeRequest request, Long currentUserId) {
        String normalizedGroupId = normalizeIdentity(groupId);
        validateRequest(request, true, null);
        ensureCodeGroupExists(normalizedGroupId);
        String codeValue = normalizeIdentity(request.getCodeValue());
        if (mapper.findDetailCode(normalizedGroupId, codeValue) != null) {
            throw validation("이미 등록된 상세코드입니다.", "codeValue", "중복된 코드값은 등록할 수 없습니다.");
        }
        validateParent(normalizedGroupId, codeValue, request.getParentCodeValue());
        mapper.insertDetailCode(
                normalizedGroupId,
                codeValue,
                request.getCodeName().trim(),
                normalizeOptionalIdentity(request.getParentCodeValue()),
                request.getSortOrder(),
                null,
                normalizeUseYn(request.getSystemUseYn()),
                request.getValidStartDate(),
                request.getValidEndDate(),
                currentUserId,
                request.getChangeReason().trim());
        return mapper.findDetailCode(normalizedGroupId, codeValue);
    }

    @Transactional
    public DetailCodeRow updateDetailCode(String groupId, String codeValue, DetailCodeRequest request, Long currentUserId) {
        String normalizedGroupId = normalizeIdentity(groupId);
        String normalizedPathCodeValue = normalizeIdentity(codeValue);
        validateRequest(request, false, normalizedPathCodeValue);
        ensureCodeGroupExists(normalizedGroupId);
        DetailCodeRow current = mapper.findDetailCode(normalizedGroupId, normalizedPathCodeValue);
        if (current == null) {
            throw validation("상세코드를 찾을 수 없습니다.", "codeValue", "등록된 상세코드만 수정할 수 있습니다.");
        }
        validateParent(normalizedGroupId, normalizedPathCodeValue, request.getParentCodeValue());
        mapper.updateDetailCode(
                normalizedGroupId,
                normalizedPathCodeValue,
                request.getCodeName().trim(),
                normalizeOptionalIdentity(request.getParentCodeValue()),
                request.getSortOrder(),
                normalizeUseYn(request.getSystemUseYn()),
                request.getValidStartDate(),
                request.getValidEndDate(),
                currentUserId,
                request.getChangeReason().trim());
        return mapper.findDetailCode(normalizedGroupId, normalizedPathCodeValue);
    }

    private void validateRequest(DetailCodeRequest request, boolean create, String pathCodeValue) {
        List<ValidationError> fields = new ArrayList<>();
        request.getUnexpectedFields().stream()
                .filter(IMMUTABLE_OR_OUT_OF_SCOPE_FIELDS::contains)
                .map(field -> new ValidationError(field, "상세코드 관리 화면에서 수정할 수 없는 필드입니다."))
                .forEach(fields::add);
        if (!hasText(request.getCodeValue())) {
            fields.add(new ValidationError("codeValue", "코드값을 입력하세요."));
        } else {
            String requestCodeValue = normalizeIdentity(request.getCodeValue());
            if (!requestCodeValue.matches("^[A-Z0-9_]+$")) {
                fields.add(new ValidationError("codeValue", "코드값은 영문 대문자, 숫자, 밑줄만 사용할 수 있습니다."));
            }
            if (!create && !requestCodeValue.equals(pathCodeValue)) {
                fields.add(new ValidationError("codeValue", "코드값은 상세코드의 식별자이므로 수정할 수 없습니다."));
            }
        }
        if (!hasText(request.getCodeName())) {
            fields.add(new ValidationError("codeName", "코드명을 입력하세요."));
        }
        if (request.getSortOrder() == null || request.getSortOrder() < 0) {
            fields.add(new ValidationError("sortOrder", "정렬순서는 0 이상이어야 합니다."));
        }
        if (!hasText(request.getChangeReason())) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        String useYn = normalizeUseYn(request.getSystemUseYn());
        if (!Set.of("Y", "N").contains(useYn)) {
            fields.add(new ValidationError("systemUseYn", "사용여부는 Y 또는 N이어야 합니다."));
        }
        LocalDate validStart = request.getValidStartDate();
        LocalDate validEnd = request.getValidEndDate();
        if (validStart != null && validEnd != null && validEnd.isBefore(validStart)) {
            fields.add(new ValidationError("validEndDate", "유효 종료일은 시작일보다 빠를 수 없습니다."));
        }
        if (request.hasAdditionalAttributes()) {
            fields.add(new ValidationError("additionalAttributes", "REQ-062 미확정으로 추가속성 구조는 임의 저장할 수 없습니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("상세코드 요청이 올바르지 않습니다.", fields);
        }
    }

    private void ensureCodeGroupExists(String groupId) {
        CodeGroupRow group = mapper.findCodeGroupById(groupId);
        if (group == null) {
            throw validation("코드그룹을 찾을 수 없습니다.", "groupId", "등록된 코드그룹의 상세코드만 관리할 수 있습니다.");
        }
        if (!"Y".equals(group.systemUseYn()) || "DELETED".equals(group.status())) {
            throw validation("사용 가능한 코드그룹이 아닙니다.", "groupId", "비활성 또는 삭제된 코드그룹에는 상세코드를 저장할 수 없습니다.");
        }
    }

    private void validateParent(String groupId, String codeValue, String parentCodeValue) {
        if (!hasText(parentCodeValue)) {
            return;
        }
        String normalizedParentCodeValue = normalizeIdentity(parentCodeValue);
        if (codeValue.equals(normalizedParentCodeValue)) {
            throw validation("상위 상세코드가 올바르지 않습니다.", "parentCodeValue", "자기 자신을 상위코드로 지정할 수 없습니다.");
        }
        if (mapper.findDetailCode(groupId, normalizedParentCodeValue) == null) {
            throw validation("상위 상세코드를 찾을 수 없습니다.", "parentCodeValue", "같은 코드그룹의 등록된 코드값만 상위코드로 지정할 수 있습니다.");
        }
        if (mapper.countChildDetailCodes(groupId, codeValue) > 0) {
            throw validation("상세코드 계층이 올바르지 않습니다.", "parentCodeValue", "하위코드를 가진 상세코드는 상위코드를 변경할 수 없습니다.");
        }
    }

    private BusinessValidationException validation(String message, String field, String fieldMessage) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, fieldMessage)));
    }

    private String normalizeIdentity(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizeOptionalIdentity(String value) {
        return hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String normalizeUseYn(String value) {
        return hasText(value) ? value.trim().toUpperCase() : "Y";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
