package kr.ac.knue.commonfoundation.codes;

import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetailCodeUsageManagementService {
    private final DetailCodeUsageManagementMapper mapper;

    public DetailCodeUsageManagementService(DetailCodeUsageManagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DetailCodeUsageSearchResponse listDetailCodeUsageSettings(String groupId, int page, int size) {
        String normalizedGroupId = normalizeIdentity(groupId);
        ensureCodeGroupExists(normalizedGroupId);
        DetailCodeUsageSearchCriteria criteria = new DetailCodeUsageSearchCriteria(normalizedGroupId, page, size);
        return new DetailCodeUsageSearchResponse(
                mapper.listDetailCodeUsageSettings(normalizedGroupId, criteria.safeSize(), criteria.offset()),
                mapper.listSelectableDetailCodesForNewInput(normalizedGroupId),
                criteria.safePage(),
                criteria.safeSize(),
                mapper.countDetailCodeUsageSettings(normalizedGroupId));
    }

    @Transactional
    public List<DetailCodeUsageRow> saveDetailCodeUsageSettings(String groupId,
                                                                DetailCodeUsageSettingsRequest request,
                                                                Long adminUserId) {
        String normalizedGroupId = normalizeIdentity(groupId);
        List<ValidationError> fields = validate(normalizedGroupId, request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("코드 사용 설정 저장 요청이 올바르지 않습니다.", fields);
        }
        List<DetailCodeUsageRow> savedRows = new ArrayList<>();
        for (DetailCodeUsageSettingsRequest.Item item : request.items()) {
            String codeValue = normalizeIdentity(item.codeValue());
            mapper.updateDetailCodeUsageSetting(normalizedGroupId, codeValue, normalizeUseYn(item.systemUseYn()),
                    item.validStartDate(), item.validEndDate(), adminUserId, item.changeReason().trim());
            savedRows.add(mapper.findDetailCodeUsageSetting(normalizedGroupId, codeValue));
        }
        return savedRows;
    }

    private List<ValidationError> validate(String groupId, DetailCodeUsageSettingsRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (groupId == null || groupId.isBlank()) {
            fields.add(new ValidationError("groupId", "코드그룹을 입력하세요."));
            return fields;
        }
        CodeGroupRow group = mapper.findCodeGroupById(groupId);
        if (group == null) {
            fields.add(new ValidationError("groupId", "등록된 코드그룹의 상세코드만 관리할 수 있습니다."));
        } else if (!"Y".equals(group.systemUseYn()) || "DELETED".equals(group.status())) {
            fields.add(new ValidationError("groupId", "비활성 또는 삭제된 코드그룹에는 코드 사용 설정을 저장할 수 없습니다."));
        }
        if (request == null || request.items() == null || request.items().isEmpty()) {
            fields.add(new ValidationError("items", "저장할 코드 사용 설정을 선택하세요."));
            return fields;
        }
        for (int index = 0; index < request.items().size(); index++) {
            DetailCodeUsageSettingsRequest.Item item = request.items().get(index);
            String prefix = "items[" + index + "].";
            DetailCodeUsageRow current = null;
            if (item.codeValue() == null || item.codeValue().isBlank()) {
                fields.add(new ValidationError(prefix + "codeValue", "코드값을 선택하세요."));
            } else {
                String codeValue = normalizeIdentity(item.codeValue());
                current = mapper.findDetailCodeUsageSetting(groupId, codeValue);
                if (current == null) {
                    fields.add(new ValidationError(prefix + "codeValue", "등록된 상세코드만 사용 설정을 변경할 수 있습니다."));
                } else if (item.codeName() != null && !item.codeName().isBlank()
                        && !item.codeName().trim().equals(current.codeName())) {
                    fields.add(new ValidationError(prefix + "codeName", "코드명은 코드 사용 관리에서 변경할 수 없습니다."));
                }
            }
            if (!"Y".equals(normalizeUseYn(item.systemUseYn())) && !"N".equals(normalizeUseYn(item.systemUseYn()))) {
                fields.add(new ValidationError(prefix + "systemUseYn", "사용여부는 Y 또는 N만 선택할 수 있습니다."));
            }
            if (item.validStartDate() != null && item.validEndDate() != null && item.validEndDate().isBefore(item.validStartDate())) {
                fields.add(new ValidationError(prefix + "validEndDate", "적용 종료일은 시작일보다 빠를 수 없습니다."));
            }
            if (item.changeReason() == null || item.changeReason().isBlank()) {
                fields.add(new ValidationError(prefix + "changeReason", "변경 사유를 입력하세요."));
            }
        }
        return fields;
    }

    private void ensureCodeGroupExists(String groupId) {
        CodeGroupRow group = mapper.findCodeGroupById(groupId);
        if (group == null) {
            throw new BusinessValidationException("코드그룹을 찾을 수 없습니다.",
                    List.of(new ValidationError("groupId", "등록된 코드그룹의 상세코드만 관리할 수 있습니다.")));
        }
        if (!"Y".equals(group.systemUseYn()) || "DELETED".equals(group.status())) {
            throw new BusinessValidationException("사용 가능한 코드그룹이 아닙니다.",
                    List.of(new ValidationError("groupId", "비활성 또는 삭제된 코드그룹에는 코드 사용 설정을 저장할 수 없습니다.")));
        }
    }

    private String normalizeIdentity(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizeUseYn(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
