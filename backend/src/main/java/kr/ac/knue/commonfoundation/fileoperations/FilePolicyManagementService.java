package kr.ac.knue.commonfoundation.fileoperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FilePolicyManagementService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private static final Pattern EXTENSION_TOKEN = Pattern.compile("^[A-Za-z0-9]+$");
    private final FilePolicyMapper mapper;

    public FilePolicyManagementService(FilePolicyMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public FilePolicySearchResponse listFilePolicies(int page, int size, String filter) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        FilePolicySearchCriteria criteria = new FilePolicySearchCriteria(normalizedPage, normalizedSize, blankToNull(filter));
        return new FilePolicySearchResponse(
                mapper.searchFilePolicies(criteria),
                normalizedPage,
                normalizedSize,
                mapper.countFilePolicies(criteria));
    }

    @Transactional
    public FilePolicyRow saveFilePolicy(FilePolicySaveRequest request, Long currentUserId) {
        List<ValidationError> fields = validateRequest(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("파일정책 요청이 올바르지 않습니다.", fields);
        }
        String businessType = request.getBusinessType().trim();
        mapper.upsertFilePolicy(
                businessType,
                normalizeExtensions(request.getAllowedExtensions()),
                request.getMaxFileSizeMb(),
                request.getMaxFilesPerItem(),
                request.getMaxTotalSizeMb(),
                request.getMaxFilenameLength(),
                Boolean.TRUE.equals(request.getMalwareScanEnabled()) ? "Y" : "N",
                currentUserId);
        return mapper.findByBusinessType(businessType);
    }

    private List<ValidationError> validateRequest(FilePolicySaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (!hasText(request.getBusinessType())) {
            fields.add(new ValidationError("businessType", "업무구분을 입력하세요."));
        }
        if (!hasText(request.getAllowedExtensions())) {
            fields.add(new ValidationError("allowedExtensions", "허용 확장자를 하나 이상 입력하세요."));
        } else {
            List<String> invalidTokens = extensionTokens(request.getAllowedExtensions()).stream()
                    .filter(token -> !EXTENSION_TOKEN.matcher(token).matches())
                    .toList();
            if (!invalidTokens.isEmpty()) {
                fields.add(new ValidationError("allowedExtensions", "확장자는 영문/숫자만 쉼표로 구분해 입력하세요."));
            }
        }
        if (request.getMaxFileSizeMb() == null || request.getMaxFileSizeMb() <= 0) {
            fields.add(new ValidationError("maxFileSizeMb", "단일 파일 최대용량은 1MB 이상이어야 합니다."));
        }
        if (request.getMaxFilesPerItem() == null || request.getMaxFilesPerItem() <= 0) {
            fields.add(new ValidationError("maxFilesPerItem", "건당 첨부개수는 1개 이상이어야 합니다."));
        }
        if (request.getMaxTotalSizeMb() != null && request.getMaxTotalSizeMb() <= 0) {
            fields.add(new ValidationError("maxTotalSizeMb", "전체용량은 1MB 이상이어야 합니다."));
        }
        if (request.getMaxFilenameLength() == null || request.getMaxFilenameLength() <= 0) {
            fields.add(new ValidationError("maxFilenameLength", "파일명 길이는 1자 이상이어야 합니다."));
        }
        if (request.getMalwareScanEnabled() == null) {
            fields.add(new ValidationError("malwareScanEnabled", "악성파일 검사 적용여부를 선택하세요."));
        }
        return fields;
    }

    private String normalizeExtensions(String raw) {
        return String.join(",", extensionTokens(raw));
    }

    static List<String> extensionTokens(String raw) {
        return List.of(raw.split(",")).stream()
                .map(String::trim)
                .map(token -> token.startsWith(".") ? token.substring(1) : token)
                .filter(token -> !token.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
