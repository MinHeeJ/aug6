package kr.ac.knue.commonfoundation.codes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodeGroupManagementService {
    private static final Set<String> IMMUTABLE_OR_OUT_OF_SCOPE_FIELDS = Set.of("status", "createdAt", "createdBy", "updatedAt", "updatedBy", "detailCodes", "additionalAttributes");
    private final CodeGroupManagementMapper mapper;

    public CodeGroupManagementService(CodeGroupManagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CodeGroupRow> listCodeGroups(int page, int size, String groupIdFilter, String filter) {
        return mapper.listCodeGroups(new CodeGroupSearchCriteria(page, size, blankToNull(groupIdFilter), blankToNull(filter)));
    }

    @Transactional
    public CodeGroupRow createCodeGroup(CodeGroupRequest request, Long currentUserId) {
        validateRequest(request, true, null);
        String groupId = normalizeGroupId(request.getGroupId());
        if (mapper.findCodeGroupById(groupId) != null) {
            throw validation("이미 등록된 코드그룹입니다.", "groupId", "중복된 그룹ID는 등록할 수 없습니다.");
        }
        mapper.insertCodeGroup(
                groupId,
                request.getGroupName().trim(),
                trimToNull(request.getDescription()),
                request.getManagingDepartment().trim(),
                normalizeUseYn(request.getSystemUseYn()),
                currentUserId,
                request.getChangeReason().trim());
        return mapper.findCodeGroupById(groupId);
    }

    @Transactional
    public CodeGroupRow updateCodeGroup(String groupId, CodeGroupRequest request, Long currentUserId) {
        String normalizedPathGroupId = normalizeGroupId(groupId);
        validateRequest(request, false, normalizedPathGroupId);
        CodeGroupRow current = mapper.findCodeGroupById(normalizedPathGroupId);
        if (current == null) {
            throw validation("코드그룹을 찾을 수 없습니다.", "groupId", "등록된 코드그룹만 수정할 수 있습니다.");
        }
        mapper.updateCodeGroup(
                normalizedPathGroupId,
                request.getGroupName().trim(),
                trimToNull(request.getDescription()),
                request.getManagingDepartment().trim(),
                normalizeUseYn(request.getSystemUseYn()),
                currentUserId,
                request.getChangeReason().trim());
        return mapper.findCodeGroupById(normalizedPathGroupId);
    }

    private void validateRequest(CodeGroupRequest request, boolean create, String pathGroupId) {
        List<ValidationError> fields = new ArrayList<>();
        if (!request.getUnexpectedFields().isEmpty()) {
            request.getUnexpectedFields().stream()
                    .filter(IMMUTABLE_OR_OUT_OF_SCOPE_FIELDS::contains)
                    .map(field -> new ValidationError(field, "코드그룹 관리 화면에서 수정할 수 없는 필드입니다."))
                    .forEach(fields::add);
        }
        if (!hasText(request.getGroupId())) {
            fields.add(new ValidationError("groupId", "그룹ID를 입력하세요."));
        } else {
            String requestGroupId = normalizeGroupId(request.getGroupId());
            if (!requestGroupId.matches("^[A-Z0-9_]+$")) {
                fields.add(new ValidationError("groupId", "그룹ID는 영문 대문자, 숫자, 밑줄만 사용할 수 있습니다."));
            }
            if (!create && !requestGroupId.equals(pathGroupId)) {
                fields.add(new ValidationError("groupId", "REQ-056 미확정으로 수정 요청의 그룹ID는 URL 그룹ID와 같아야 합니다."));
            }
        }
        if (!hasText(request.getGroupName())) {
            fields.add(new ValidationError("groupName", "명칭을 입력하세요."));
        }
        if (!hasText(request.getManagingDepartment())) {
            fields.add(new ValidationError("managingDepartment", "관리부서를 입력하세요."));
        }
        if (!hasText(request.getChangeReason())) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        String useYn = normalizeUseYn(request.getSystemUseYn());
        if (!Set.of("Y", "N").contains(useYn)) {
            fields.add(new ValidationError("systemUseYn", "사용여부는 Y 또는 N이어야 합니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("코드그룹 요청이 올바르지 않습니다.", fields);
        }
    }

    private BusinessValidationException validation(String message, String field, String fieldMessage) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, fieldMessage)));
    }

    private String normalizeGroupId(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizeUseYn(String value) {
        return hasText(value) ? value.trim().toUpperCase() : "Y";
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
