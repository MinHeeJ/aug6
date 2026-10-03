package kr.ac.knue.commonfoundation.lectureimprovement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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
 * Coordinates scoped lecture-improvement reads and atomic writes, including the
 * shared input-period/finalization guards and immutable data-change history.
 */
@Service
public class LectureImprovementService {
    private static final List<String> READ_ROLES = List.of("R01", "R02", "R04");
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

    /** Returns rows exposed by the existing education-achievement data scope. */
    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(
            LectureImprovementSearchCriteria criteria,
            CurrentUser requester) {
        requireReadRole(requester);
        LectureImprovementSearchCriteria safeCriteria = criteria == null
                ? new LectureImprovementSearchCriteria(0, 20)
                : criteria;
        return new LectureImprovementSearchResponse(
                mapper.listLectureImprovements(safeCriteria, requester.userId(), requester.roles()),
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.countLectureImprovements(safeCriteria, requester.userId(), requester.roles()));
    }

    /** Finds one row only after applying the same ownership/data-scope policy as list. */
    @Transactional(readOnly = true)
    public LectureImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        LectureImprovementRow row = mapper.findAccessibleLectureImprovement(
                achievementId,
                requester.userId(),
                requester.roles());
        if (row == null) {
            LectureImprovementRow existing = mapper.findLectureImprovement(achievementId);
            if (existing == null) {
                throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
            }
            throw new ForbiddenException();
        }
        return row;
    }

    /** Creates a DRAFT row after the shared mutation guard validates the caller and period. */
    @Transactional
    public LectureImprovementRow create(
            LectureImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriteRole(requester);
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        request.academicYear().toString(),
                        request.achievementDate()));
        String managementNo = "LI-" + UUID.randomUUID();
        mapper.insertLectureImprovement(
                managementNo,
                requester.userId(),
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.achievementContent().trim(),
                request.academicYear(),
                request.semester(),
                serializeAttachmentIds(request.attachmentIds()),
                requester.userId());
        LectureImprovementRow saved = mapper.findLectureImprovementByManagementNo(managementNo);
        if (saved == null) {
            throw new NotFoundException("저장한 강의개선 실적을 찾을 수 없습니다.");
        }
        mapper.insertChangeHistory(
                "teaching_improvement_achievements",
                String.valueOf(saved.achievementId()),
                "CREATE",
                "achievement_content",
                null,
                saved.achievementContent(),
                requester.userId(),
                "강의개선 실적 저장",
                requestId);
        return saved;
    }

    /** Updates a caller-owned draft row only after finalization and input-period checks. */
    @Transactional
    public LectureImprovementRow update(
            Long achievementId,
            LectureImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriteRole(requester);
        LectureImprovementRow existing = find(achievementId);
        if (!requester.userId().equals(existing.targetUserId())) {
            throw new ForbiddenException();
        }
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.targetUserId(),
                        request.academicYear().toString(),
                        request.achievementDate()));
        mapper.updateLectureImprovement(
                achievementId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.achievementContent().trim(),
                request.academicYear(),
                request.semester(),
                serializeAttachmentIds(request.attachmentIds()),
                requester.userId());
        LectureImprovementRow saved = find(achievementId);
        mapper.insertChangeHistory(
                "teaching_improvement_achievements",
                String.valueOf(achievementId),
                "UPDATE",
                "achievement_content",
                existing.achievementContent(),
                saved.achievementContent(),
                requester.userId(),
                "강의개선 실적 수정",
                requestId);
        return saved;
    }

    private LectureImprovementRow find(Long achievementId) {
        LectureImprovementRow row = mapper.findLectureImprovement(achievementId);
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void validate(LectureImprovementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "강의개선 실적 정보를 입력하세요."));
        } else {
            if (blankToNull(request.managementItemCode()) == null) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.achievementDate() == null) {
                errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
            }
            if (blankToNull(request.achievementContent()) == null) {
                errors.add(new ValidationError("achievementContent", "강의개선 내용을 입력하세요."));
            }
            if (request.academicYear() == null || request.academicYear() < 2000) {
                errors.add(new ValidationError("academicYear", "학년도는 2000 이상이어야 합니다."));
            }
            if (request.semester() == null || (request.semester() != 1 && request.semester() != 2)) {
                errors.add(new ValidationError("semester", "학기는 1 또는 2만 입력할 수 있습니다."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("강의개선 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(READ_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
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
}
