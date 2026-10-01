package kr.ac.knue.commonfoundation.basic81;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns lecture-achievement reads and atomic create/update transactions, including
 * the required status and data-change history side effects.
 */
@Service
public class LectureAchievementService {
    private static final String ACHIEVEMENT_TYPE = "LECTURE";
    private final LectureAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public LectureAchievementService(
            LectureAchievementMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Returns the caller-scoped lecture rows for the requested dynamic filters. */
    @Transactional(readOnly = true)
    public LectureAchievementSearchResponse list(
            LectureAchievementSearchCriteria criteria,
            CurrentUser requester) {
        requireAuthorizedUser(requester);
        LectureAchievementSearchCriteria safeCriteria = criteria == null
                ? new LectureAchievementSearchCriteria(0, 20, null, null, null, null)
                : criteria;
        return new LectureAchievementSearchResponse(
                mapper.listLectureAchievements(safeCriteria, requester.userId(), requester.roles()),
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.countLectureAchievements(safeCriteria, requester.userId(), requester.roles()));
    }

    /**
     * Creates or updates a lecture row after the shared data-scope, input-period,
     * and evaluation-finalization guards have completed before any mutation.
     */
    @Transactional
    public LectureAchievementSaveResult save(
            SaveLectureAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireAuthorizedUser(requester);
        LectureAchievementRow existing = request.achievementId() == null
                ? null
                : findExisting(request.achievementId());
        Long targetUserId = existing == null ? requester.userId() : existing.targetUserId();
        String evaluationYear = existing == null
                ? String.valueOf(Year.from(request.occurredDate()))
                : existing.evaluationYear();
        OccurredDateValidation occurredDateValidation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(targetUserId, evaluationYear, request.occurredDate()));
        String detail = serializeDetail(request.achievementDetail());
        if (existing == null) {
            return create(request, requester, requestId, evaluationYear, detail, occurredDateValidation);
        }
        return update(request, requester, requestId, detail, existing, occurredDateValidation);
    }

    private LectureAchievementSaveResult create(
            SaveLectureAchievementRequest request,
            CurrentUser requester,
            String requestId,
            String evaluationYear,
            String detail,
            OccurredDateValidation occurredDateValidation) {
        String managementNo = "LA-" + UUID.randomUUID();
        mapper.insertLectureAchievement(
                managementNo,
                requester.userId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.occurredDate(),
                detail,
                blankToNull(request.attachmentRef()),
                requester.userId());
        LectureAchievementRow saved = findSaved(managementNo, null);
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                ACHIEVEMENT_TYPE,
                saved.achievementId(),
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "강의실적 최초 입력",
                requester.userId(),
                LocalDateTime.now()));
        mapper.insertChangeHistory(
                "lecture_achievements",
                String.valueOf(saved.achievementId()),
                "CREATE",
                "achievement_detail",
                null,
                saved.achievementDetail(),
                requester.userId(),
                "강의실적 저장",
                requestId);
        return new LectureAchievementSaveResult(
                saved,
                occurredDateValidation.warning(),
                occurredDateValidation.message());
    }

    private LectureAchievementSaveResult update(
            SaveLectureAchievementRequest request,
            CurrentUser requester,
            String requestId,
            String detail,
            LectureAchievementRow existing,
            OccurredDateValidation occurredDateValidation) {
        mapper.updateLectureAchievement(
                existing.achievementId(),
                request.managementItemCode().trim(),
                request.occurredDate(),
                detail,
                blankToNull(request.attachmentRef()),
                requester.userId());
        LectureAchievementRow saved = findSaved(null, existing.achievementId());
        mapper.insertChangeHistory(
                "lecture_achievements",
                String.valueOf(existing.achievementId()),
                "UPDATE",
                "achievement_detail",
                existing.achievementDetail(),
                saved.achievementDetail(),
                requester.userId(),
                "강의실적 수정",
                requestId);
        return new LectureAchievementSaveResult(
                saved,
                occurredDateValidation.warning(),
                occurredDateValidation.message());
    }

    private LectureAchievementRow findExisting(Long achievementId) {
        LectureAchievementRow existing = mapper.findLectureAchievement(achievementId);
        if (existing == null) {
            throw new NotFoundException("강의실적을 찾을 수 없습니다.");
        }
        return existing;
    }

    private LectureAchievementRow findSaved(String managementNo, Long achievementId) {
        LectureAchievementRow saved = achievementId == null
                ? mapper.findLectureAchievementByManagementNo(managementNo)
                : mapper.findLectureAchievement(achievementId);
        if (saved == null) {
            throw new NotFoundException(
                    managementNo == null
                            ? "수정한 강의실적을 찾을 수 없습니다."
                            : "저장한 강의실적을 찾을 수 없습니다.");
        }
        return saved;
    }

    private void validate(SaveLectureAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "강의실적 정보를 입력하세요."));
        } else {
            if (blankToNull(request.managementItemCode()) == null) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.occurredDate() == null) {
                errors.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("강의실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private void requireAuthorizedUser(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private String serializeDetail(JsonNode detail) {
        if (detail == null || detail.isNull()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "강의실적 상세 입력값이 올바르지 않습니다.",
                    List.of(new ValidationError("achievementDetail", "상세 입력값을 확인하세요.")));
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
