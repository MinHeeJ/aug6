package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * Implements the lecture-evaluation vertical slice so achievement writes, audit rows, and state
 * histories are committed atomically after the shared access gate has approved the mutation.
 */
@Service
public class LectureEvaluationAchievementService {
    private static final String TARGET_BUSINESS = "lecture_evaluation_achievements";
    private final LectureEvaluationAchievementMapper mapper;
    private final EducationAchievementAccessValidator accessValidator;
    private final EducationAchievementStatusTransitionPolicy transitionPolicy;
    private final ObjectMapper objectMapper;

    public LectureEvaluationAchievementService(
            LectureEvaluationAchievementMapper mapper,
            EducationAchievementAccessValidator accessValidator,
            EducationAchievementStatusTransitionPolicy transitionPolicy,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.accessValidator = accessValidator;
        this.transitionPolicy = transitionPolicy;
        this.objectMapper = objectMapper;
    }

    /** Returns only rows the caller's data scope is permitted to inspect. */
    @Transactional(readOnly = true)
    public LectureEvaluationAchievementSearchResponse list(
            LectureEvaluationAchievementSearchCriteria criteria,
            CurrentUser actor
    ) {
        requireAchievementRole(actor);
        LectureEvaluationAchievementSearchCriteria normalized = normalizeCriteria(criteria, actor);
        return new LectureEvaluationAchievementSearchResponse(
                mapper.list(normalized),
                normalized.safePage(),
                normalized.safeSize(),
                mapper.count(normalized)
        );
    }

    /** Creates or updates an achievement and preserves its previous business representation. */
    @Transactional
    public LectureEvaluationAchievementRow save(
            SaveLectureEvaluationAchievementRequest request,
            CurrentUser actor
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "강의평가 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("request", "저장할 강의평가를 입력하세요."))
            );
        }
        String detail = serializeDetail(request);
        String reason = trimToNull(request.changeReason());
        if (request.achievementId() == null) {
            Long targetUserId = request.targetUserId() == null ? actor.userId() : request.targetUserId();
            String evaluationYear = normalizeYear(request.evaluationYear(), request.occurredDate());
            String organizationCode = resolveOrganizationCode(request.organizationCode(), targetUserId);
            LectureEvaluationAchievementValidationResult warning = validateAccess(
                    actor,
                    targetUserId,
                    evaluationYear,
                    organizationCode,
                    request.occurredDate()
            );
            String managementNo = "LE-" + evaluationYear + "-" + UUID.randomUUID();
            mapper.insertAchievement(
                    managementNo,
                    targetUserId,
                    evaluationYear,
                    organizationCode,
                    request.managementItemCode().trim(),
                    request.occurredDate(),
                    detail,
                    trimToNull(request.attachmentRef()),
                    actor.userId(),
                    reason
            );
            LectureEvaluationAchievementRow saved = mapper.findByManagementNo(managementNo);
            mapper.insertChangeHistory(saved.achievementId(), "CREATE", "achievement", null, detail, actor.userId(), reasonOrDefault(reason));
            return withWarning(saved, warning.occurredDateWarning());
        }
        LectureEvaluationAchievementRow current = requireMutable(request.achievementId());
        LectureEvaluationAchievementValidationResult warning = validateAccess(
                actor,
                current.teacherUserId(),
                current.evaluationYear(),
                current.organizationCode(),
                current.occurredDate()
        );
        mapper.updateAchievement(
                current.achievementId(),
                request.managementItemCode().trim(),
                request.occurredDate(),
                detail,
                trimToNull(request.attachmentRef()),
                actor.userId(),
                reason
        );
        mapper.insertChangeHistory(current.achievementId(), "UPDATE", "achievement", current.achievementDetail(), detail, actor.userId(), reasonOrDefault(reason));
        return withWarning(mapper.findById(current.achievementId()), warning.occurredDateWarning());
    }

    /** Applies only a graph-approved certification action and appends the required status history. */
    @Transactional
    public LectureEvaluationAchievementRow transition(
            Long achievementId,
            LectureEvaluationAchievementTransitionRequest request,
            CurrentUser actor,
            String requestId
    ) {
        LectureEvaluationAchievementRow current = requireMutable(achievementId);
        validateAccess(actor, current.teacherUserId(), current.evaluationYear(), current.organizationCode(), current.occurredDate());
        EducationAchievementStatusTransition transition = transitionPolicy.transition(
                current.certificationStatus(),
                request.actionType(),
                request.reasonCode(),
                request.opinion(),
                actor.userId(),
                LocalDateTime.now()
        );
        String reason = trimToNull(request.changeReason());
        mapper.updateCertificationStatus(current.achievementId(), transition.nextStatus(), actor.userId(), reason);
        mapper.insertStatusHistory(
                current.achievementId(),
                transition.previousStatus(),
                transition.nextStatus(),
                transition.actionType(),
                transition.reasonCode(),
                transition.opinion(),
                transition.processedBy(),
                transition.processedAt(),
                reasonOrDefault(reason),
                requestId
        );
        mapper.insertChangeHistory(current.achievementId(), "UPDATE", "certificationStatus", transition.previousStatus(), transition.nextStatus(), actor.userId(), reasonOrDefault(reason));
        return mapper.findById(current.achievementId());
    }

    private LectureEvaluationAchievementRow requireMutable(Long achievementId) {
        LectureEvaluationAchievementRow current = mapper.findById(achievementId);
        if (current == null) {
            throw new NotFoundException("강의평가 실적을 찾을 수 없습니다.");
        }
        if ("EVALUATION_CONFIRMED".equals(current.certificationStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정된 실적은 변경할 수 없습니다.");
        }
        return current;
    }

    private LectureEvaluationAchievementValidationResult validateAccess(CurrentUser actor, Long targetUserId, String evaluationYear, String organizationCode, LocalDate occurredDate) {
        EducationAchievementValidationResult result = accessValidator.validateMutation(
                actor,
                new EducationAchievementMutationContext(targetUserId, evaluationYear, organizationCode, occurredDate, LocalDateTime.now())
        );
        return new LectureEvaluationAchievementValidationResult(result.occurredDateWarning());
    }

    private LectureEvaluationAchievementSearchCriteria normalizeCriteria(LectureEvaluationAchievementSearchCriteria criteria, CurrentUser actor) {
        LectureEvaluationAchievementSearchCriteria source = criteria == null
                ? new LectureEvaluationAchievementSearchCriteria(0, 20, null, null, null, null, null, null, null, null)
                : criteria;
        String role = actor.roles().contains("R04") || actor.roles().contains("R09") ? "R04" : actor.roles().contains("R02") ? "R02" : "R01";
        return new LectureEvaluationAchievementSearchCriteria(source.page(), source.size(), trimToNull(source.managementNo()), trimToNull(source.teacherName()), trimToNull(source.managementItemCode()), source.occurredDateFrom(), source.occurredDateTo(), trimToNull(source.certificationStatus()), actor.userId(), role);
    }

    private void requireAchievementRole(CurrentUser actor) {
        if (actor == null || actor.roles() == null || actor.roles().stream().noneMatch(
                role -> role.equals("R01") || role.equals("R02") || role.equals("R04") || role.equals("R09")
        )) {
            throw new kr.ac.knue.commonfoundation.common.api.ForbiddenException();
        }
    }

    private String normalizeYear(String evaluationYear, LocalDate occurredDate) {
        String value = trimToNull(evaluationYear);
        return value == null && occurredDate != null ? String.valueOf(occurredDate.getYear()) : value;
    }

    private String resolveOrganizationCode(String requestedOrganizationCode, Long targetUserId) {
        String value = trimToNull(requestedOrganizationCode);
        return value == null ? mapper.findActiveOrganizationCode(targetUserId) : value.toUpperCase();
    }

    private String serializeDetail(SaveLectureEvaluationAchievementRequest request) {
        try {
            return request.achievementDetail() == null ? "{}" : objectMapper.writeValueAsString(request.achievementDetail());
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException("강의평가 상세정보 형식이 올바르지 않습니다.", List.of(new ValidationError("achievementDetail", "상세정보를 확인하세요.")));
        }
    }

    private LectureEvaluationAchievementRow withWarning(LectureEvaluationAchievementRow row, boolean warning) {
        return row;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String reasonOrDefault(String reason) {
        return reason == null ? "강의평가 실적 처리" : reason;
    }

    private record LectureEvaluationAchievementValidationResult(boolean occurredDateWarning) {
    }
}
