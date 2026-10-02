package kr.ac.knue.commonfoundation.basic81;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates lecture-evaluation listing and save transactions so source data,
 * initial status history, and change history are committed atomically.
 */
@Service
public class LectureEvaluationAchievementService {
    private static final String ACHIEVEMENT_TYPE = "LECTURE_EVALUATION";
    private final LectureEvaluationAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final EducationAchievementStatusTransitionPolicy transitionPolicy;
    private final ObjectMapper objectMapper;

    public LectureEvaluationAchievementService(
            LectureEvaluationAchievementMapper mapper,
            EducationAchievementGuardService guardService,
            EducationAchievementStatusTransitionPolicy transitionPolicy,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.transitionPolicy = transitionPolicy;
        this.objectMapper = objectMapper;
    }

    /** Returns only rows visible within the caller's existing education-achievement data scope. */
    @Transactional(readOnly = true)
    public LectureEvaluationAchievementSearchResponse list(
            LectureEvaluationAchievementSearchCriteria criteria,
            CurrentUser requester) {
        requireAuthorizedUser(requester);
        LectureEvaluationAchievementSearchCriteria safeCriteria = criteria == null
                ? new LectureEvaluationAchievementSearchCriteria(0, 20, null, null, null, null, null, null)
                : criteria;
        return new LectureEvaluationAchievementSearchResponse(
                mapper.listLectureEvaluationAchievements(safeCriteria, requester.userId(), requester.roles()),
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.countLectureEvaluationAchievements(safeCriteria, requester.userId(), requester.roles()));
    }

    /**
     * Creates a draft lecture-evaluation achievement after shared scope, input-period,
     * finalization-lock, and occurred-date checks. A period warning is returned with
     * the saved row because the approved contract allows that save.
     */
    @Transactional
    public LectureEvaluationAchievementSaveResult save(
            SaveLectureEvaluationAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        List<ValidationError> errors = validate(request);
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("강의평가 실적 저장 요청이 올바르지 않습니다.", errors);
        }
        String evaluationYear = String.valueOf(Year.from(request.occurredDate()));
        OccurredDateValidation occurredDateValidation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.occurredDate()));
        String managementNo = "LE-" + UUID.randomUUID();
        mapper.insertLectureEvaluationAchievement(
                managementNo,
                requester.userId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.occurredDate(),
                serializeDetail(request),
                blankToNull(request.attachmentRef()),
                requester.userId());
        LectureEvaluationAchievementRow saved = mapper.findLectureEvaluationAchievement(managementNo);
        if (saved == null) {
            throw new NotFoundException("저장한 강의평가 실적을 찾을 수 없습니다.");
        }
        EducationAchievementStatusHistory initialHistory = new EducationAchievementStatusHistory(
                ACHIEVEMENT_TYPE,
                saved.achievementId(),
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "강의평가 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now());
        mapper.insertStatusHistory(initialHistory);
        mapper.insertChangeHistory(
                "lecture_evaluation_achievements",
                String.valueOf(saved.achievementId()),
                "CREATE",
                "achievement_detail",
                null,
                saved.achievementDetail(),
                requester.userId(),
                "강의평가 실적 저장",
                requestId);
        return new LectureEvaluationAchievementSaveResult(saved, occurredDateValidation.warning(), occurredDateValidation.message());
    }

    /**
     * Appends a permitted lifecycle record after checking the source row's actual
     * current status; callers use this shared operation for later transition UI/API work.
     */
    @Transactional
    public EducationAchievementStatusHistory transitionStatus(
            Long achievementId,
            EducationAchievementStatus nextStatus,
            String actionType,
            String reasonCode,
            String opinion,
            CurrentUser requester) {
        requireAuthorizedUser(requester);
        String storedStatus = mapper.findCertificationStatus(achievementId);
        if (storedStatus == null) {
            throw new NotFoundException("강의평가 실적을 찾을 수 없습니다.");
        }
        EducationAchievementStatus currentStatus = EducationAchievementStatus.valueOf(storedStatus);
        EducationAchievementStatusHistory history = transitionPolicy.transition(
                new EducationAchievementStatusTransitionRequest(
                        ACHIEVEMENT_TYPE,
                        achievementId,
                        currentStatus,
                        nextStatus,
                        actionType,
                        reasonCode,
                        opinion,
                        requester.userId(),
                        LocalDateTime.now()));
        mapper.updateCertificationStatus(achievementId, nextStatus.name(), requester.userId());
        mapper.insertStatusHistory(history);
        return history;
    }

    private List<ValidationError> validate(SaveLectureEvaluationAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "강의평가 실적 정보를 입력하세요."));
            return errors;
        }
        if (blankToNull(request.managementItemCode()) == null) {
            errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
        }
        if (request.occurredDate() == null) {
            errors.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
        }
        return errors;
    }

    private void requireAuthorizedUser(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new kr.ac.knue.commonfoundation.common.api.ForbiddenException();
        }
    }

    private String serializeDetail(SaveLectureEvaluationAchievementRequest request) {
        if (request.achievementDetail() == null || request.achievementDetail().isNull()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(request.achievementDetail());
        } catch (JsonProcessingException exception) {
            throw new ConflictException("강의평가 상세 입력값을 저장할 수 없습니다.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
