package kr.ac.knue.commonfoundation.manuals;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualManagementService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private static final Set<String> ALLOWED_MANUAL_TYPES = Set.of("USER", "ADMIN");
    private final ManualManagementMapper mapper;

    public ManualManagementService(ManualManagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ManualSearchResponse listManuals(int page, int size, String manualType, String targetUser, LocalDate effectiveDate) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        String normalizedType = normalizeTypeOrNull(manualType);
        String normalizedTarget = blankToNull(targetUser);
        LocalDate asOf = effectiveDate == null ? LocalDate.now() : effectiveDate;
        List<ManualRow> manuals = mapper.listManuals(normalizedType, normalizedTarget, asOf, safeSize, safePage * safeSize);
        long total = mapper.countManuals(normalizedType, normalizedTarget, asOf);
        return new ManualSearchResponse(manuals, safePage, safeSize, total);
    }

    @Transactional
    public ManualRow createManual(ManualCreateRequest request, Long currentUserId) {
        validateCreateRequest(request);
        String manualType = request.getManualType().trim().toUpperCase();
        String version = request.getVersion().trim();
        String targetUser = request.getTargetUser().trim().toUpperCase();
        ManualRow duplicate = mapper.findDuplicate(manualType, targetUser, version);
        if (duplicate != null) {
            throw new BusinessValidationException("동일 유형·대상 사용자·버전 매뉴얼이 이미 존재합니다.",
                    List.of(new ValidationError("version", "동일 유형·대상 사용자에 같은 버전을 중복 등록할 수 없습니다.")));
        }
        mapper.insertManual(manualType, version, targetUser, request.getEffectiveDate(), currentUserId, request.getChangeReason().trim());
        Long manualId = mapper.lastManualId();
        mapper.insertManualFile(manualId, request.getOriginalFileName().trim(),
                UUID.randomUUID().toString(), request.getFileContent().getBytes(StandardCharsets.UTF_8), currentUserId);
        return mapper.findManual(manualId, request.getEffectiveDate());
    }

    @Transactional(readOnly = true)
    public ManualDownload downloadManualFile(Long manualId) {
        ManualDownload download = mapper.findDownload(manualId);
        if (download == null) {
            throw new NotFoundException("다운로드할 매뉴얼 파일이 없습니다.");
        }
        return download;
    }

    private void validateCreateRequest(ManualCreateRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        request.getUnexpectedFields().forEach(field -> fields.add(new ValidationError(field, "매뉴얼 관리에서 허용하지 않는 필드입니다.")));
        String type = request.getManualType() == null ? null : request.getManualType().trim().toUpperCase();
        if (type != null && !type.isBlank() && !ALLOWED_MANUAL_TYPES.contains(type)) {
            fields.add(new ValidationError("manualType", "허용된 매뉴얼 유형이 아닙니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("매뉴얼 등록 요청이 올바르지 않습니다.", fields);
        }
    }

    private String normalizeTypeOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
