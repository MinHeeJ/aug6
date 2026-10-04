package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns caller-scoped 취업률 제고 reads and atomic writes to the BASIC-83 header,
 * detail, and immutable change-history records.
 */
@Service
public class EmploymentRateImprovementService {
    private static final String ACHIEVEMENT_TYPE = "EMPLOYMENT_RATE_IMPROVEMENT";
    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public EmploymentRateImprovementService(
            EmploymentRateImprovementMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Lists only rows that the caller can read through the established education scope. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            EmploymentRateImprovementSearchCriteria criteria,
            CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateImprovementSearchCriteria safe = criteria == null
                ? new EmploymentRateImprovementSearchCriteria(0, 20)
                : criteria;
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(safe, requester.userId(), requester.roles()),
                safe.safePage(),
                safe.safePageSize(),
                mapper.count(requester.userId(), requester.roles()));
    }

    /** Retrieves one data-scoped row or returns the standard not-found/forbidden result. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateImprovementRow row = findExisting(achievementId);
        if (!mapper.isVisible(achievementId, requester.userId(), requester.roles())) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Creates a DRAFT achievement only after common scope, period, and finalization guards pass. */
    @Transactional
    public EmploymentRateImprovementSaveResponse create(
            EmploymentRateImprovementRequest request,
            CurrentUser requester) {
        requireWriteRole(requester);
        validate(request);
        OccurredDateValidation period = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        Long achievementId = mapper.insertAchievement(
                "ERI-" + UUID.randomUUID(),
                requester.userId(),
                String.valueOf(Year.from(request.achievementDate())),
                request.managementItemCode().trim(),
                request.achievementDate(),
                serializeAttachments(request.attachmentIds()),
                requester.userId());
        mapper.insertDetail(
                achievementId,
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                trimToNull(request.mockExamQuestionPeriod()));
        EmploymentRateImprovementRow saved = findExisting(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "CREATE",
                null,
                saved.managementItemCode(),
                requester.userId(),
                "취업률 제고 실적 등록");
        return new EmploymentRateImprovementSaveResponse(saved, period.warning(), period.message());
    }

    /** Updates a caller-owned draftable row atomically after all shared guards pass. */
    @Transactional
    public EmploymentRateImprovementSaveResponse update(
            Long achievementId,
            EmploymentRateImprovementRequest request,
            CurrentUser requester) {
        requireWriteRole(requester);
        validate(request);
        EmploymentRateImprovementRow existing = findExisting(achievementId);
        if (!existing.teacherUserId().equals(requester.userId())) {
            throw new ForbiddenException();
        }
        OccurredDateValidation period = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        existing.evaluationYear(),
                        request.achievementDate()));
        mapper.updateAchievement(
                achievementId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                serializeAttachments(request.attachmentIds()),
                requester.userId());
        mapper.updateDetail(
                achievementId,
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                trimToNull(request.mockExamQuestionPeriod()));
        EmploymentRateImprovementRow saved = findExisting(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                existing.managementItemCode(),
                saved.managementItemCode(),
                requester.userId(),
                "취업률 제고 실적 수정");
        return new EmploymentRateImprovementSaveResponse(saved, period.warning(), period.message());
    }

    private EmploymentRateImprovementRow findExisting(Long achievementId) {
        EmploymentRateImprovementRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void validate(EmploymentRateImprovementRequest request) {
        if (request != null
                && request.specialLectureStartDate() != null
                && request.specialLectureEndDate() != null
                && request.specialLectureEndDate().isBefore(request.specialLectureStartDate())) {
            throw new BusinessValidationException(
                    "특강 종료일은 시작일보다 빠를 수 없습니다.",
                    List.of(new ValidationError("specialLectureEndDate", "특강 종료일을 확인하세요.")));
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private String serializeAttachments(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(attachmentIds == null ? List.of() : attachmentIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 식별자가 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 식별자를 확인하세요.")));
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
