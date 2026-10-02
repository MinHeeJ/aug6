package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDateTime;
import java.time.Year;
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

/** Owns scoped teaching-improvement reads and atomic guarded persistence with audit history. */
@Service
public class LectureImprovementAchievementService {
    private static final String TYPE = "LECTURE_IMPROVEMENT";

    private final LectureImprovementAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;

    public LectureImprovementAchievementService(
            LectureImprovementAchievementMapper mapper,
            EducationAchievementGuardService guardService) {
        this.mapper = mapper;
        this.guardService = guardService;
    }

    /** Lists only teaching-improvement achievements visible to the caller's role scope. */
    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(
            LectureImprovementSearchCriteria criteria,
            CurrentUser requester) {
        requireReadRole(requester);
        LectureImprovementSearchCriteria safe = criteria == null
                ? new LectureImprovementSearchCriteria(0, 20)
                : criteria;
        return new LectureImprovementSearchResponse(
                mapper.list(safe, requester.userId(), requester.roles()),
                safe.safePage(),
                safe.safePageSize(),
                mapper.count(safe, requester.userId(), requester.roles())
        );
    }

    /** Finds one row only after applying the same caller data scope as the list. */
    @Transactional(readOnly = true)
    public LectureImprovementAchievementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        LectureImprovementAchievementRow row = mapper.findInScope(
                achievementId,
                requester.userId(),
                requester.roles()
        );
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Creates an R01-owned DRAFT row and its lifecycle/change-history records atomically. */
    @Transactional
    public LectureImprovementSaveResult create(
            LectureImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        requireWriteRole(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()
                )
        );
        String managementNo = "LIA-" + UUID.randomUUID();
        mapper.insert(
                managementNo,
                requester.userId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.achievementContent().trim(),
                request.academicYear(),
                request.semester(),
                attachmentRef(request.attachmentIds()),
                requester.userId()
        );
        LectureImprovementAchievementRow saved = findByManagementNo(managementNo);
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                TYPE,
                saved.achievementId(),
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "강의개선 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now()
        ));
        mapper.insertChangeHistory(
                "teaching_improvement_achievements",
                String.valueOf(saved.achievementId()),
                "CREATE",
                "achievement",
                null,
                saved.managementNo(),
                requester.userId(),
                "강의개선 실적 저장",
                requestId
        );
        return result(saved, warning);
    }

    /** Updates an R01-owned row only after the shared scope, period, and finalization guards. */
    @Transactional
    public LectureImprovementSaveResult update(
            Long achievementId,
            LectureImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        requireWriteRole(requester);
        LectureImprovementAchievementRow existing = find(achievementId);
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.targetUserId(),
                        existing.evaluationYear(),
                        request.achievementDate()
                )
        );
        mapper.update(
                existing.achievementId(),
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.achievementContent().trim(),
                request.academicYear(),
                request.semester(),
                attachmentRef(request.attachmentIds()),
                requester.userId()
        );
        LectureImprovementAchievementRow saved = find(achievementId);
        mapper.insertChangeHistory(
                "teaching_improvement_achievements",
                String.valueOf(saved.achievementId()),
                "UPDATE",
                "achievement",
                existing.managementNo(),
                saved.managementNo(),
                requester.userId(),
                "강의개선 실적 수정",
                requestId
        );
        return result(saved, warning);
    }

    private LectureImprovementAchievementRow find(Long achievementId) {
        LectureImprovementAchievementRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private LectureImprovementAchievementRow findByManagementNo(String managementNo) {
        LectureImprovementAchievementRow row = mapper.findByManagementNo(managementNo);
        if (row == null) {
            throw new NotFoundException("저장한 강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private LectureImprovementSaveResult result(
            LectureImprovementAchievementRow row,
            OccurredDateValidation warning) {
        return new LectureImprovementSaveResult(row, warning.warning(), warning.message());
    }

    private void validateRequest(LectureImprovementRequest request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "강의개선 실적 정보를 입력하세요.",
                    List.of(new ValidationError("body", "필수입니다."))
            );
        }
        if (request.managementItemCode() == null || request.managementItemCode().isBlank()
                || request.achievementDate() == null || request.achievementContent() == null
                || request.achievementContent().isBlank() || request.academicYear() == null
                || request.semester() == null) {
            throw new BusinessValidationException("강의개선 실적 저장 요청이 올바르지 않습니다.", List.of());
        }
        if (request.academicYear() < 2000) {
            throw new BusinessValidationException(
                    "강의개선 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("academicYear", "학년도는 2000 이상이어야 합니다."))
            );
        }
        if (request.semester() != 1 && request.semester() != 2) {
            throw new BusinessValidationException(
                    "강의개선 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("semester", "학기는 1 또는 2여야 합니다."))
            );
        }
        if (request.attachmentIds() != null && request.attachmentIds().size() > 1) {
            throw new BusinessValidationException(
                    "강의개선 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "현재 첨부 참조는 한 건만 저장할 수 있습니다."))
            );
        }
    }

    private void requireReadRole(CurrentUser user) {
        if (user == null || user.userId() == null || user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04", "R09").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser user) {
        if (user == null || user.userId() == null || user.roles() == null
                || !user.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private String attachmentRef(List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty() || attachmentIds.get(0) == null
                || attachmentIds.get(0).isBlank()) {
            return null;
        }
        return attachmentIds.get(0).trim();
    }
}
