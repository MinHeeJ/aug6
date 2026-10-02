package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns caller-scoped 취업률 reads and guarded atomic writes, recording the
 * status and data-change histories required for the new achievement source.
 */
@Service
public class EmploymentRateAchievementService {
    private static final String ACHIEVEMENT_TYPE = "EMPLOYMENT_RATE";
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardService guardService) {
        this.mapper = mapper;
        this.guardService = guardService;
    }

    /** Returns only records in the requester's established education-achievement scope. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementSearchResponse list(
            EmploymentRateAchievementSearchCriteria criteria,
            CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateAchievementSearchCriteria safeCriteria = criteria == null
                ? new EmploymentRateAchievementSearchCriteria(0, 20, null, null, null, null)
                : criteria;
        return new EmploymentRateAchievementSearchResponse(
                mapper.list(safeCriteria, requester.userId(), requester.roles()),
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.count(safeCriteria, requester.userId(), requester.roles()));
    }

    /** Returns a detail row after applying the same role and data-scope rules as mutation guards. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateAchievementRow row = mapper.findInScope(
                achievementId,
                requester.userId(),
                requester.roles());
        if (row == null) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Creates a new own-target DRAFT row after all input period and finalization checks pass. */
    @Transactional
    public EmploymentRateAchievementSaveResult create(
            EmploymentRateAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        requireWriteRole(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation dateValidation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        String managementNo = "ERA-" + UUID.randomUUID();
        mapper.insert(
                managementNo,
                requester.userId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                trimToNull(request.achievementName()),
                singleAttachmentRef(request.attachmentIds()),
                requester.userId());
        EmploymentRateAchievementRow saved = mapper.findByManagementNo(managementNo);
        if (saved == null) {
            throw new NotFoundException("저장한 취업률 실적을 찾을 수 없습니다.");
        }
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                ACHIEVEMENT_TYPE,
                saved.achievementId(),
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "취업률 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now()));
        mapper.insertChangeHistory(
                "employment_rate_achievements",
                String.valueOf(saved.achievementId()),
                "CREATE",
                "achievement",
                null,
                saved.managementNo(),
                requester.userId(),
                "취업률 실적 저장",
                requestId);
        return result(saved, dateValidation);
    }

    /** Updates only an R01-owned row after the shared guard rejects locked or inactive input. */
    @Transactional
    public EmploymentRateAchievementSaveResult update(
            Long achievementId,
            EmploymentRateAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        requireWriteRole(requester);
        EmploymentRateAchievementRow existing = find(achievementId);
        OccurredDateValidation dateValidation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.targetUserId(),
                        existing.evaluationYear(),
                        request.achievementDate()));
        mapper.update(
                existing.achievementId(),
                request.managementItemCode().trim(),
                request.achievementDate(),
                trimToNull(request.achievementName()),
                singleAttachmentRef(request.attachmentIds()),
                requester.userId());
        EmploymentRateAchievementRow saved = find(achievementId);
        mapper.insertChangeHistory(
                "employment_rate_achievements",
                String.valueOf(saved.achievementId()),
                "UPDATE",
                "achievement",
                existing.managementNo(),
                saved.managementNo(),
                requester.userId(),
                "취업률 실적 수정",
                requestId);
        return result(saved, dateValidation);
    }

    /**
     * Returns an actual Office Open XML workbook for the authorized download scope.
     * R07 is the dedicated Excel operator; the other roles retain their established
     * education-achievement scopes.
     */
    @Transactional(readOnly = true)
    public byte[] download(
            EmploymentRateAchievementSearchCriteria criteria,
            CurrentUser requester) {
        requireDownloadRole(requester);
        EmploymentRateAchievementSearchCriteria safeCriteria = criteria == null
                ? new EmploymentRateAchievementSearchCriteria(0, 20, null, null, null, null)
                : criteria;
        return EmploymentRateAchievementWorkbook.create(
                mapper.listForDownload(
                        safeCriteria,
                        requester.userId(),
                        requester.roles()));
    }

    /** Returns a persisted bulk-job receipt and its per-target outcomes for the R07 operator. */
    @Transactional(readOnly = true)
    public EmploymentRateBulkJobResponse getBulkJob(String jobId, CurrentUser requester) {
        requireExcelRole(requester);
        EmploymentRateBulkJobRow job = mapper.findBulkJob(jobId);
        if (job == null) {
            throw new NotFoundException("취업률 일괄 작업을 찾을 수 없습니다.");
        }
        List<EmploymentRateBulkJobItemRow> items = mapper.listBulkJobItems(jobId);
        long processedCount = items.stream()
                .filter(item -> "PROCESSED".equals(item.processingStatus()))
                .count();
        long skippedCount = items.stream()
                .filter(item -> "SKIPPED".equals(item.processingStatus()))
                .count();
        long failedCount = items.stream()
                .filter(item -> "FAILED".equals(item.processingStatus()))
                .count();
        return new EmploymentRateBulkJobResponse(
                job.jobId(),
                job.evaluationYear(),
                job.actionType(),
                job.status(),
                job.requestedBy(),
                job.requestedAt(),
                job.completedAt(),
                items.size(),
                processedCount,
                skippedCount,
                failedCount,
                items);
    }

    private EmploymentRateAchievementRow find(Long achievementId) {
        EmploymentRateAchievementRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private EmploymentRateAchievementSaveResult result(
            EmploymentRateAchievementRow row,
            OccurredDateValidation validation) {
        return new EmploymentRateAchievementSaveResult(
                row,
                validation.warning(),
                validation.message());
    }

    private void validateRequest(EmploymentRateAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "취업률 실적 정보를 입력하세요."));
        } else {
            if (trimToNull(request.managementItemCode()) == null) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.achievementDate() == null) {
                errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
            }
            if (request.attachmentIds() != null && request.attachmentIds().size() > 1) {
                errors.add(new ValidationError("attachmentIds", "현재 첨부 참조는 한 건만 저장할 수 있습니다."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("취업률 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04", "R09").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireDownloadRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(
                        role -> List.of("R01", "R02", "R04", "R07", "R09").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireExcelRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R07")) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private String singleAttachmentRef(List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return null;
        }
        return trimToNull(attachmentIds.get(0));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
