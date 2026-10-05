package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates caller-scoped lecture-improvement reads and atomic header/detail
 * mutations, including lifecycle guards and immutable change-history evidence.
 */
@Service
public class LectureImprovementService {
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

    /** Returns only records visible through the caller's established education data scope. */
    @Transactional(readOnly = true)
    public LectureImprovementListResponse list(int page, int pageSize, CurrentUser requester) {
        requireReadRole(requester);
        return new LectureImprovementListResponse(
                mapper.list(requester.userId(), requester.roles(), pageSize, page * pageSize),
                page,
                pageSize,
                mapper.count(requester.userId(), requester.roles()));
    }

    /** Returns one record after verifying both its existence and the caller's data scope. */
    @Transactional(readOnly = true)
    public LectureImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        LectureImprovementRow row = findExisting(achievementId);
        if (mapper.countReadableBy(achievementId, requester.userId(), requester.roles()) == 0) {
            throw new ForbiddenException();
        }
        return row;
    }

    /**
     * Creates the common header and the lecture-improvement detail in one
     * transaction after shared input-period, scope, and finalization checks.
     */
    @Transactional
    public LectureImprovementRow create(
            LectureImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        requireWriteRole(requester);
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        String.valueOf(request.academicYear()),
                        request.achievementDate()));
        String managementNo = "LI-" + UUID.randomUUID();
        mapper.insertAchievement(
                managementNo,
                requester.userId(),
                String.valueOf(request.academicYear()),
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentIdsJson(request.attachmentIds()),
                requester.userId());
        Long achievementId = mapper.findIdByManagementNo(managementNo);
        if (achievementId == null) {
            throw new NotFoundException("저장한 강의개선 실적을 찾을 수 없습니다.");
        }
        mapper.insertDetail(
                achievementId,
                request.achievementContent().trim(),
                request.academicYear(),
                String.valueOf(request.semester()),
                requester.userId());
        mapper.insertInitialStatusHistory(achievementId, requester.userId());
        LectureImprovementRow result = findExisting(achievementId);
        mapper.insertChangeHistory(
                "education_achievements",
                String.valueOf(result.achievementId()),
                "CREATE",
                "lecture_improvement",
                null,
                describe(result),
                requester.userId(),
                "강의개선 실적 등록");
        return result;
    }

    /** Updates a draft/submitted caller-owned record after shared guards have run. */
    @Transactional
    public LectureImprovementRow update(
            Long achievementId,
            LectureImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        requireWriteRole(requester);
        LectureImprovementRow existing = findExisting(achievementId);
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        String.valueOf(existing.academicYear()),
                        request.achievementDate()));
        mapper.updateAchievement(
                achievementId,
                String.valueOf(request.academicYear()),
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentIdsJson(request.attachmentIds()),
                requester.userId());
        mapper.updateDetail(
                achievementId,
                request.achievementContent().trim(),
                request.academicYear(),
                String.valueOf(request.semester()),
                requester.userId());
        LectureImprovementRow result = findExisting(achievementId);
        mapper.insertChangeHistory(
                "education_achievements",
                String.valueOf(achievementId),
                "UPDATE",
                "lecture_improvement",
                describe(existing),
                describe(result),
                requester.userId(),
                "강의개선 실적 수정");
        return result;
    }

    private LectureImprovementRow findExisting(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        LectureImprovementRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }


    private void validateRequest(LectureImprovementRequest request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "강의개선 실적 저장 정보가 필요합니다.",
                    List.of(new ValidationError("body", "강의개선 실적 정보를 입력하세요.")));
        }
        if (request.attachmentIds() != null
                && request.attachmentIds().stream()
                        .anyMatch(value -> value == null || value.isBlank())) {
            throw new BusinessValidationException(
                    "첨부파일 식별자가 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부파일 식별자를 확인하세요.")));
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null
                || requester.userId() == null
                || requester.roles() == null
                || requester.roles().stream().noneMatch(List.of("R01", "R02", "R04")::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private String attachmentIdsJson(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(attachmentIds == null ? List.of() : attachmentIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부파일 정보가 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부파일 정보를 확인하세요.")));
        }
    }

    private String describe(LectureImprovementRow row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("강의개선 변경이력을 직렬화할 수 없습니다.", exception);
        }
    }
}
