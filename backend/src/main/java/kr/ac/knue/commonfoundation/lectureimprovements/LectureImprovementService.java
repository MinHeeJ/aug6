package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates scoped lecture-improvement reads and atomic source/detail/audit
 * writes while applying the shared education-achievement lifecycle guards.
 */
@Service
public class LectureImprovementService {
    private static final String ACHIEVEMENT_TYPE = "LECTURE_IMPROVEMENT";
    private final LectureImprovementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public LectureImprovementService(
            LectureImprovementMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Returns only rows allowed by the caller's R01, R02, or R04 data scope. */
    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(
            LectureImprovementSearchCriteria criteria,
            CurrentUser requester) {
        requireReadRole(requester);
        LectureImprovementSearchCriteria safeCriteria = criteria == null
                ? new LectureImprovementSearchCriteria(0, 20, null, null, null, null, null)
                : criteria;
        return new LectureImprovementSearchResponse(
                mapper.list(safeCriteria, requester.userId(), requester.roles()),
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.count(safeCriteria, requester.userId(), requester.roles()));
    }

    /** Retrieves one row through the same scope predicate used by the list operation. */
    @Transactional(readOnly = true)
    public LectureImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        if (achievementId == null || achievementId <= 0) {
            throw new BusinessValidationException(
                    "강의개선 실적 식별자가 올바르지 않습니다.",
                    List.of(new ValidationError("achievementId", "실적 식별자를 입력하세요.")));
        }
        LectureImprovementRow row = mapper.findScoped(achievementId, requester.userId(), requester.roles());
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /**
     * Creates a source row and its required detail/audit records in one
     * transaction after server-side field, data-scope, period, and lock checks.
     */
    @Transactional
    public LectureImprovementSaveResult create(
            LectureImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriteRole(requester);
        OccurredDateValidation dateValidation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        String.valueOf(request.academicYear()),
                        request.achievementDate()));
        String organizationCode = mapper.findOrganizationCode(requester.userId());
        if (organizationCode == null) {
            throw new ConflictException("ACTIVE_ORGANIZATION_REQUIRED: 활성 소속 정보가 필요합니다.");
        }
        String managementNo = "LI-" + UUID.randomUUID();
        String attachmentRefs = serializeAttachments(request.attachmentIds());
        mapper.insertAchievement(
                managementNo,
                requester.userId(),
                organizationCode,
                String.valueOf(request.academicYear()),
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.achievementContent().trim(),
                attachmentRefs,
                requester.userId());
        LectureImprovementRow storedSource = mapper.findByManagementNo(managementNo);
        if (storedSource == null) {
            throw new NotFoundException("저장한 강의개선 실적을 찾을 수 없습니다.");
        }
        mapper.insertDetail(
                storedSource.achievementId(),
                request.achievementContent().trim(),
                request.academicYear(),
                String.valueOf(request.semester()),
                requester.userId());
        LectureImprovementRow saved = mapper.findByManagementNo(managementNo);
        if (saved == null) {
            throw new NotFoundException("저장한 강의개선 실적을 찾을 수 없습니다.");
        }
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                ACHIEVEMENT_TYPE,
                saved.achievementId(),
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "강의개선 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now()));
        mapper.insertChangeHistory(
                "education_achievements",
                String.valueOf(saved.achievementId()),
                "CREATE",
                "lecture_improvement_achievement_details",
                null,
                request.achievementContent().trim(),
                requester.userId(),
                "강의개선 실적 저장",
                requestId,
                LocalDateTime.now());
        return new LectureImprovementSaveResult(saved, dateValidation.warning(), dateValidation.message());
    }

    /**
     * Updates an R01 caller's own unlocked row and appends an audit record;
     * no update can occur before the shared period and finalization guards pass.
     */
    @Transactional
    public LectureImprovementSaveResult update(
            Long achievementId,
            LectureImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriteRole(requester);
        LectureImprovementRow existing = get(achievementId, requester);
        if (!requester.userId().equals(existing.teacherUserId())) {
            throw new ForbiddenException();
        }
        OccurredDateValidation dateValidation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        String.valueOf(request.academicYear()),
                        request.achievementDate()));
        String attachmentRefs = serializeAttachments(request.attachmentIds());
        int sourceCount = mapper.updateAchievement(
                existing.achievementId(),
                request.managementItemCode().trim(),
                String.valueOf(request.academicYear()),
                request.achievementDate(),
                request.achievementContent().trim(),
                attachmentRefs,
                requester.userId());
        int detailCount = mapper.updateDetail(
                existing.achievementId(),
                request.achievementContent().trim(),
                request.academicYear(),
                String.valueOf(request.semester()),
                requester.userId());
        if (sourceCount != 1 || detailCount != 1) {
            throw new NotFoundException("수정할 강의개선 실적을 찾을 수 없습니다.");
        }
        LectureImprovementRow saved = mapper.findByManagementNo(existing.managementNo());
        if (saved == null) {
            throw new NotFoundException("수정한 강의개선 실적을 찾을 수 없습니다.");
        }
        mapper.insertChangeHistory(
                "education_achievements",
                String.valueOf(existing.achievementId()),
                "UPDATE",
                "lecture_improvement_achievement_details",
                existing.achievementContent(),
                request.achievementContent().trim(),
                requester.userId(),
                "강의개선 실적 수정",
                requestId,
                LocalDateTime.now());
        return new LectureImprovementSaveResult(saved, dateValidation.warning(), dateValidation.message());
    }

    private void validate(LectureImprovementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "강의개선 실적 정보를 입력하세요."));
        } else {
            if (request.academicYear() == null || request.academicYear() < 2000) {
                errors.add(new ValidationError("academicYear", "학년도는 2000 이상이어야 합니다."));
            }
            if (request.semester() == null || (request.semester() != 1 && request.semester() != 2)) {
                errors.add(new ValidationError("semester", "학기는 1 또는 2여야 합니다."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("강의개선 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private String serializeAttachments(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(attachmentIds == null ? List.of() : attachmentIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 참조값이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 참조값을 확인하세요.")));
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }
}
