package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implements caller-scoped employment-rate achievement CRUD and delegates Excel
 * validation to the shared Excel boundary. Batch execution remains unavailable
 * until the unapproved OQ-83-01 execution policy is supplied.
 */
@Service
public class EmploymentRateAchievementService {
    private static final Set<String> READ_ROLES = Set.of("R01", "R02", "R04", "R07");
    private static final String EMPLOYMENT_RATE_TEMPLATE_ID = "B83-EMPLOYMENT-RATE-ACHIEVEMENT-V1";
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ExcelOperationsService excelOperationsService;
    private final ObjectMapper objectMapper;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardService guardService,
            ExcelOperationsService excelOperationsService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.excelOperationsService = excelOperationsService;
        this.objectMapper = objectMapper;
    }

    /** Returns only records visible to the active caller's established data scope. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementSearchResponse list(
            EmploymentRateAchievementSearchCriteria criteria,
            CurrentUser requester) {
        requireRole(requester, READ_ROLES);
        EmploymentRateAchievementSearchCriteria safe = criteria == null
                ? new EmploymentRateAchievementSearchCriteria(0, 20, null, null, null)
                : criteria;
        return new EmploymentRateAchievementSearchResponse(
                mapper.list(safe, requester.userId(), requester.roles()),
                safe.safePage(),
                safe.safePageSize(),
                mapper.count(safe, requester.userId(), requester.roles()));
    }

    /** Reads one achievement only after proving the caller can access its established data scope. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementRow get(Long achievementId, CurrentUser requester) {
        requireRole(requester, READ_ROLES);
        EmploymentRateAchievementRow row = find(achievementId);
        requireReadableScope(achievementId, requester);
        return row;
    }

    /** Creates a common-header EMPLOYMENT_RATE row and its audit entry atomically. */
    @Transactional
    public EmploymentRateAchievementSaveResult create(
            EmploymentRateAchievementSaveRequest request,
            CurrentUser requester,
            String requestId) {
        requireRole(requester, Set.of("R01"));
        validate(request);
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        Long achievementId = mapper.insert(
                "ERA-" + UUID.randomUUID(),
                requester.userId(),
                String.valueOf(Year.from(request.achievementDate())),
                request.managementItemCode().trim(),
                request.achievementDate(),
                blankToNull(request.achievementName()),
                serializeAttachmentIds(request.attachmentIds()));
        if (achievementId == null) {
            throw new ConflictException("소속 정보가 없어 취업률 실적을 저장할 수 없습니다.");
        }
        mapper.insertChangeHistory(
                achievementId,
                "CREATE",
                null,
                "EMPLOYMENT_RATE",
                requester.userId(),
                "취업률 실적 저장 requestId=" + requestId);
        return new EmploymentRateAchievementSaveResult(find(achievementId), warning.warning(), warning.message());
    }

    /** Updates an R01-owned, non-finalized employment-rate record atomically. */
    @Transactional
    public EmploymentRateAchievementSaveResult update(
            Long achievementId,
            EmploymentRateAchievementSaveRequest request,
            CurrentUser requester,
            String requestId) {
        requireRole(requester, Set.of("R01"));
        validate(request);
        EmploymentRateAchievementRow existing = find(achievementId);
        requireOwnerOrFail(existing, requester);
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        existing.evaluationYear(),
                        request.achievementDate()));
        int changed = mapper.update(
                achievementId,
                requester.userId(),
                request.managementItemCode().trim(),
                request.achievementDate(),
                blankToNull(request.achievementName()),
                serializeAttachmentIds(request.attachmentIds()));
        if (changed != 1) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        mapper.insertChangeHistory(
                achievementId,
                "UPDATE",
                existing.achievementName(),
                blankToNull(request.achievementName()),
                requester.userId(),
                "취업률 실적 수정 requestId=" + requestId);
        return new EmploymentRateAchievementSaveResult(find(achievementId), warning.warning(), warning.message());
    }

    /**
     * Delegates template/version validation and immutable upload-history storage to the
     * common Excel service. The shared service owns its transaction so rejected uploads
     * retain their error artifact while this endpoint returns the contract's HTTP 400.
     */
    public ExcelUploadResult upload(MultipartFile file, CurrentUser requester) {
        requireRole(requester, Set.of("R07"));
        ExcelUploadResult result = excelOperationsService.createExcelUpload(
                "EMPLOYMENT_RATE_ACHIEVEMENT",
                EMPLOYMENT_RATE_TEMPLATE_ID,
                file,
                requester.userId());
        if (result.errorCount() > 0) {
            throw new BusinessValidationException(
                    "취업률 실적 Excel 검증에 실패했습니다.",
                    List.of(new ValidationError(
                            "file",
                            "오류 또는 중복 행이 있어 업무 데이터를 반영하지 않았습니다.")));
        }
        return result;
    }

    /** Returns a previously persisted batch result; creation is intentionally policy-blocked. */
    @Transactional(readOnly = true)
    public EmploymentRateBulkJobRow getBatchJob(String jobId, CurrentUser requester) {
        requireRole(requester, Set.of("R07"));
        EmploymentRateBulkJobRow row = mapper.findBatchJob(jobId);
        if (row == null) {
            throw new NotFoundException("취업률 일괄 처리 작업을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Prevents irreversible batch generation or deletion before OQ-83-01 is approved. */
    @Transactional
    public EmploymentRateBulkJobRow requestBatchJob(
            EmploymentRateBulkJobRequest request,
            CurrentUser requester) {
        requireRole(requester, Set.of("R07"));
        if (request == null) {
            throw new BusinessValidationException(
                    "일괄 처리 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("body", "일괄 처리 요청 본문은 필수입니다.")));
        }
        if (blankToNull(request.evaluationYear()) == null) {
            throw new BusinessValidationException(
                    "일괄 처리 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("evaluationYear", "평가연도는 필수입니다.")));
        }
        if (blankToNull(request.actionType()) == null) {
            throw new BusinessValidationException(
                    "일괄 처리 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("actionType", "처리유형은 필수입니다.")));
        }
        throw new ConflictException("OQ-83-01: 일괄 생성·삭제 실행 정책이 승인되지 않았습니다.");
    }

    private EmploymentRateAchievementRow find(Long achievementId) {
        EmploymentRateAchievementRow row = mapper.find(achievementId);
        if (row == null) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void requireOwnerOrFail(EmploymentRateAchievementRow row, CurrentUser requester) {
        if (requester.roles().contains("R01") && !requester.userId().equals(row.teacherUserId())) {
            throw new ForbiddenException();
        }
    }

    private void requireReadableScope(Long achievementId, CurrentUser requester) {
        if (!mapper.isVisible(achievementId, requester.userId(), requester.roles())) {
            throw new ForbiddenException();
        }
    }

    private void requireRole(CurrentUser user, Set<String> allowedRoles) {
        if (user == null || user.roles() == null || user.userId() == null
                || user.roles().stream().noneMatch(allowedRoles::contains)) {
            throw new ForbiddenException();
        }
    }

    private void validate(EmploymentRateAchievementSaveRequest request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "취업률 실적 요청이 필요합니다.",
                    List.of(new ValidationError("body", "요청 본문은 필수입니다.")));
        }
    }

    private String serializeAttachmentIds(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(attachmentIds == null ? List.of() : attachmentIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 참조값이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 참조값을 확인하세요.")));
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Response preserves the date-outside-period warning defined by the shared guard. */
    public record EmploymentRateAchievementSaveResult(
            EmploymentRateAchievementRow achievement,
            boolean achievementDateWarning,
            String warningMessage) {
    }
}
