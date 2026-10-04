package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates scoped lecture-improvement reads and atomic writes so detail data,
 * lifecycle history, and change history never persist independently.
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

    /** Returns the caller-scoped page using only the approved page and pageSize filters. */
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
                mapper.count(safe, requester.userId(), requester.roles()));
    }

    /** Reads a row through the same scope predicate used for the list. */
    @Transactional(readOnly = true)
    public LectureImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        LectureImprovementRow row = mapper.findVisible(achievementId, requester.userId(), requester.roles());
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Creates a draft header/detail pair and its histories in one transaction. */
    @Transactional
    public LectureImprovementSaveResponse create(
            LectureImprovementSaveRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriteRole(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation validation = guard(request, requester, evaluationYear);
        LectureImprovementInsertCommand command = new LectureImprovementInsertCommand(
                "LI-" + UUID.randomUUID(),
                requester.userId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentJson(request),
                requester.userId());
        mapper.insertAchievement(command);
        Long achievementId = command.getAchievementId();
        if (achievementId == null) {
            throw new ConflictException("소속 정보가 없어 강의개선 실적을 저장할 수 없습니다.");
        }
        mapper.insertDetail(
                achievementId,
                request.achievementContent().trim(),
                request.academicYear(),
                request.semesterCode());
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                ACHIEVEMENT_TYPE,
                achievementId,
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "강의개선 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now()));
        LectureImprovementRow saved = get(achievementId, requester);
        mapper.insertChangeHistory(
                "education_achievements",
                achievementId.toString(),
                "CREATE",
                "lecture_improvement_achievement_details",
                null,
                saved.achievementContent(),
                requester.userId(),
                "강의개선 실적 저장 (" + requestId + ")");
        return response(saved, validation);
    }

    /** Updates the path-selected detail only after the shared scope, period, and finalization guards. */
    @Transactional
    public LectureImprovementSaveResponse update(
            Long achievementId,
            LectureImprovementSaveRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriteRole(requester);
        LectureImprovementRow existing = get(achievementId, requester);
        String requestedEvaluationYear = String.valueOf(Year.from(request.achievementDate()));
        if (!existing.evaluationYear().equals(requestedEvaluationYear)) {
            throw new ConflictException("EVALUATION_YEAR_CHANGE_NOT_ALLOWED: 업적발생일은 동일 평가연도 내에서만 변경할 수 있습니다.");
        }
        OccurredDateValidation validation = guard(request, requester, existing.evaluationYear());
        mapper.updateAchievement(
                achievementId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentJson(request),
                requester.userId());
        mapper.updateDetail(
                achievementId,
                request.achievementContent().trim(),
                request.academicYear(),
                request.semesterCode());
        LectureImprovementRow saved = get(achievementId, requester);
        recordChangedFields(existing, saved, requester.userId(), requestId);
        return response(saved, validation);
    }

    /**
     * Persists one immutable record for each changed persisted value so later reviews can reconstruct
     * the exact header and detail mutation without relying on a mutable current row.
     */
    private void recordChangedFields(
            LectureImprovementRow existing,
            LectureImprovementRow saved,
            Long changedBy,
            String requestId) {
        recordChangedField(
                saved,
                "managementItemCode",
                existing.managementItemCode(),
                saved.managementItemCode(),
                changedBy,
                requestId);
        recordChangedField(
                saved,
                "achievementDate",
                stringValue(existing.achievementDate()),
                stringValue(saved.achievementDate()),
                changedBy,
                requestId);
        recordChangedField(
                saved,
                "achievementContent",
                existing.achievementContent(),
                saved.achievementContent(),
                changedBy,
                requestId);
        recordChangedField(
                saved,
                "academicYear",
                stringValue(existing.academicYear()),
                stringValue(saved.academicYear()),
                changedBy,
                requestId);
        recordChangedField(
                saved,
                "semesterCode",
                stringValue(existing.semester()),
                stringValue(saved.semester()),
                changedBy,
                requestId);
        recordChangedField(
                saved,
                "attachmentIds",
                attachmentJson(existing.attachmentIds()),
                attachmentJson(saved.attachmentIds()),
                changedBy,
                requestId);
    }

    private void recordChangedField(
            LectureImprovementRow saved,
            String fieldName,
            String beforeValue,
            String afterValue,
            Long changedBy,
            String requestId) {
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory(
                    "education_achievements",
                    saved.achievementId().toString(),
                    "UPDATE",
                    fieldName,
                    beforeValue,
                    afterValue,
                    changedBy,
                    "강의개선 실적 수정 (" + requestId + ")");
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private OccurredDateValidation guard(
            LectureImprovementSaveRequest request,
            CurrentUser requester,
            String evaluationYear) {
        return guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
    }

    private LectureImprovementSaveResponse response(
            LectureImprovementRow row,
            OccurredDateValidation validation) {
        return new LectureImprovementSaveResponse(row, validation.warning(), validation.message());
    }

    private String attachmentJson(LectureImprovementSaveRequest request) {
        return attachmentJson(request.attachmentIds());
    }

    private String attachmentJson(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(
                    attachmentIds == null ? Collections.emptyList() : attachmentIds);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("첨부 식별자 형식이 올바르지 않습니다.");
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
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
}
