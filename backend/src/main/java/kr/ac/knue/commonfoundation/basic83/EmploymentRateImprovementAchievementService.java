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
 * Owns caller-scoped 취업률 제고 reads and guarded atomic writes, recording the
 * status and data-change histories required for the new achievement source.
 */
@Service
public class EmploymentRateImprovementAchievementService {
    private static final String ACHIEVEMENT_TYPE = "EMPLOYMENT_RATE_IMPROVEMENT";
    private final EmploymentRateImprovementAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;

    public EmploymentRateImprovementAchievementService(
            EmploymentRateImprovementAchievementMapper mapper,
            EducationAchievementGuardService guardService) {
        this.mapper = mapper;
        this.guardService = guardService;
    }

    /** Returns only records in the requester's established education-achievement scope. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            EmploymentRateImprovementSearchCriteria criteria,
            CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateImprovementSearchCriteria safeCriteria = criteria == null
                ? new EmploymentRateImprovementSearchCriteria(0, 20, null, null, null, null)
                : criteria;
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(safeCriteria, requester.userId(), requester.roles()),
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.count(safeCriteria, requester.userId(), requester.roles()));
    }

    /** Returns a detail row after applying the same role and data-scope rules as mutation guards. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateImprovementRow row = mapper.findInScope(
                achievementId,
                requester.userId(),
                requester.roles());
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Creates a new own-target DRAFT row after all input period and finalization checks pass. */
    @Transactional
    public EmploymentRateImprovementSaveResult create(
            EmploymentRateImprovementRequest request,
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
        String managementNo = "ERI-" + UUID.randomUUID();
        mapper.insert(
                managementNo,
                requester.userId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                trimToNull(request.mockExamQuestionPeriod()),
                singleAttachmentRef(request.attachmentIds()),
                requester.userId());
        EmploymentRateImprovementRow saved = mapper.findByManagementNo(managementNo);
        if (saved == null) {
            throw new NotFoundException("저장한 취업률 제고 실적을 찾을 수 없습니다.");
        }
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                ACHIEVEMENT_TYPE,
                saved.achievementId(),
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "취업률 제고 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now()));
        mapper.insertChangeHistory(
                "employment_rate_improvement_achievements",
                String.valueOf(saved.achievementId()),
                "CREATE",
                "achievement",
                null,
                saved.managementNo(),
                requester.userId(),
                "취업률 제고 실적 저장",
                requestId);
        return result(saved, dateValidation);
    }

    /** Updates only an R01-owned row after the shared guard rejects locked or inactive input. */
    @Transactional
    public EmploymentRateImprovementSaveResult update(
            Long achievementId,
            EmploymentRateImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        requireWriteRole(requester);
        EmploymentRateImprovementRow existing = find(achievementId);
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
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                trimToNull(request.mockExamQuestionPeriod()),
                singleAttachmentRef(request.attachmentIds()),
                requester.userId());
        EmploymentRateImprovementRow saved = find(achievementId);
        mapper.insertChangeHistory(
                "employment_rate_improvement_achievements",
                String.valueOf(saved.achievementId()),
                "UPDATE",
                "achievement",
                existing.managementNo(),
                saved.managementNo(),
                requester.userId(),
                "취업률 제고 실적 수정",
                requestId);
        return result(saved, dateValidation);
    }

    private EmploymentRateImprovementRow find(Long achievementId) {
        EmploymentRateImprovementRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private EmploymentRateImprovementSaveResult result(
            EmploymentRateImprovementRow row,
            OccurredDateValidation validation) {
        return new EmploymentRateImprovementSaveResult(
                row,
                validation.warning(),
                validation.message());
    }

    private void validateRequest(EmploymentRateImprovementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "취업률 제고 실적 정보를 입력하세요."));
        } else {
            if (trimToNull(request.managementItemCode()) == null) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.achievementDate() == null) {
                errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
            }
            if (request.specialLectureStartDate() != null
                    && request.specialLectureEndDate() != null
                    && request.specialLectureEndDate().isBefore(request.specialLectureStartDate())) {
                errors.add(new ValidationError("specialLectureEndDate", "특강 종료일은 시작일보다 빠를 수 없습니다."));
            }
            if (request.attachmentIds() != null && request.attachmentIds().size() > 1) {
                errors.add(new ValidationError("attachmentIds", "현재 첨부 참조는 한 건만 저장할 수 있습니다."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("취업률 제고 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04", "R09").contains(role))) {
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
