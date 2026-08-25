package kr.ac.knue.commonfoundation.helpcontents;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HelpContentManagementService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private final HelpContentManagementMapper mapper;

    public HelpContentManagementService(HelpContentManagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public HelpContentSearchResponse listHelpContents(int page, int size, String screenId) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        String normalizedScreenId = blankToNull(screenId);
        List<HelpContentRow> rows = mapper.listHelpContents(normalizedScreenId, safeSize, safePage * safeSize);
        long total = mapper.countHelpContents(normalizedScreenId);
        return new HelpContentSearchResponse(rows, safePage, safeSize, total);
    }

    @Transactional
    public HelpContentRow saveHelpContent(String screenId, HelpContentSaveRequest request, Long currentUserId) {
        String normalizedScreenId = normalizeScreenId(screenId);
        validateSaveRequest(normalizedScreenId, request);
        mapper.upsertHelpContent(normalizedScreenId,
                request.getBusinessDescription().trim(),
                request.getInputCriteria().trim(),
                trimOrEmpty(request.getFaq()),
                trimOrEmpty(request.getContact()),
                currentUserId,
                request.getChangeReason().trim());
        return mapper.findHelpContentRow(normalizedScreenId);
    }

    @Transactional(readOnly = true)
    public HelpContentResponse getHelpContent(String screenId) {
        HelpContentResponse helpContent = mapper.findHelpContent(normalizeScreenId(screenId));
        if (helpContent == null) {
            throw new NotFoundException("등록된 도움말이 없습니다.");
        }
        return helpContent;
    }

    private void validateSaveRequest(String screenId, HelpContentSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (screenId.isBlank()) {
            fields.add(new ValidationError("screenId", "화면ID를 입력하세요."));
        }
        request.getUnexpectedFields().forEach(field -> fields.add(new ValidationError(field, "도움말 관리에서 허용하지 않는 필드입니다.")));
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("도움말 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private String normalizeScreenId(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim().toUpperCase();
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
