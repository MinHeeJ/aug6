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
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists lecture achievements through the shared access gate so source rows, change history, and
 * certification history remain consistent with the education-achievement lifecycle.
 */
@Service
public class LectureAchievementService {
    private final LectureAchievementMapper mapper;
    private final EducationAchievementAccessValidator accessValidator;
    private final EducationAchievementStatusTransitionPolicy transitionPolicy;
    private final ObjectMapper objectMapper;

    public LectureAchievementService(
            LectureAchievementMapper mapper,
            EducationAchievementAccessValidator accessValidator,
            EducationAchievementStatusTransitionPolicy transitionPolicy,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.accessValidator = accessValidator;
        this.transitionPolicy = transitionPolicy;
        this.objectMapper = objectMapper;
    }

    /** Returns persisted lecture rows filtered to the authenticated user's data scope. */
    @Transactional(readOnly = true)
    public LectureAchievementSearchResponse list(
            LectureAchievementSearchCriteria criteria,
            CurrentUser actor
    ) {
        requireRole(actor);
        LectureAchievementSearchCriteria normalized = normalizeCriteria(criteria, actor);
        return new LectureAchievementSearchResponse(
                mapper.list(normalized),
                normalized.safePage(),
                normalized.safeSize(),
                mapper.count(normalized)
        );
    }

    /** Creates or updates a lecture achievement and records the business representation change. */
    @Transactional
    public LectureAchievementRow save(SaveLectureAchievementRequest request, CurrentUser actor) {
        if (request == null) {
            throw new BusinessValidationException(
                    "강의실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("request", "저장할 강의실적을 입력하세요."))
            );
        }
        requireRole(actor);
        String detail = detail(request);
        String reason = text(request.changeReason());
        if (request.achievementId() == null) {
            Long targetUserId = request.targetUserId() == null ? actor.userId() : request.targetUserId();
            String evaluationYear = year(request.evaluationYear(), request.occurredDate());
            String organizationCode = organization(request.organizationCode(), targetUserId);
            accessValidator.validateMutation(
                    actor,
                    new EducationAchievementMutationContext(
                            targetUserId,
                            evaluationYear,
                            organizationCode,
                            request.occurredDate(),
                            LocalDateTime.now()
                    )
            );
            String managementNo = "LA-" + evaluationYear + "-" + UUID.randomUUID();
            mapper.insertAchievement(
                    managementNo, targetUserId, evaluationYear, organizationCode,
                    request.managementItemCode().trim(), request.occurredDate(), detail,
                    text(request.attachmentRef()), actor.userId(), reason
            );
            LectureAchievementRow saved = mapper.findByManagementNo(managementNo);
            mapper.insertChangeHistory(
                    saved.achievementId(), "CREATE", "achievement", null, detail,
                    actor.userId(), reasonOrDefault(reason)
            );
            return saved;
        }
        LectureAchievementRow current = mutable(request.achievementId());
        accessValidator.validateMutation(
                actor,
                new EducationAchievementMutationContext(
                        current.teacherUserId(),
                        current.evaluationYear(),
                        current.organizationCode(),
                        current.occurredDate(),
                        LocalDateTime.now()
                )
        );
        mapper.updateAchievement(
                current.achievementId(), request.managementItemCode().trim(), request.occurredDate(),
                detail, text(request.attachmentRef()), actor.userId(), reason
        );
        mapper.insertChangeHistory(
                current.achievementId(), "UPDATE", "achievement", current.achievementDetail(),
                detail, actor.userId(), reasonOrDefault(reason)
        );
        return mapper.findById(current.achievementId());
    }

    /** Applies a graph-approved certification action and appends immutable status history. */
    @Transactional
    public LectureAchievementRow transition(
            Long achievementId,
            LectureAchievementTransitionRequest request,
            CurrentUser actor,
            String requestId
    ) {
        requireRole(actor);
        LectureAchievementRow current = mutable(achievementId);
        accessValidator.validateMutation(actor, new EducationAchievementMutationContext(
                current.teacherUserId(), current.evaluationYear(), current.organizationCode(),
                current.occurredDate(), LocalDateTime.now()
        ));
        EducationAchievementStatusTransition transition = transitionPolicy.transition(
                current.certificationStatus(), request.actionType(), request.reasonCode(), request.opinion(),
                actor.userId(), LocalDateTime.now()
        );
        String reason = text(request.changeReason());
        mapper.updateCertificationStatus(current.achievementId(), transition.nextStatus(), actor.userId(), reason);
        mapper.insertStatusHistory(
                current.achievementId(), transition.previousStatus(), transition.nextStatus(),
                transition.actionType(), transition.reasonCode(), transition.opinion(),
                transition.processedBy(), transition.processedAt(), reasonOrDefault(reason), requestId
        );
        mapper.insertChangeHistory(
                current.achievementId(), "UPDATE", "certificationStatus", transition.previousStatus(),
                transition.nextStatus(), actor.userId(), reasonOrDefault(reason)
        );
        return mapper.findById(current.achievementId());
    }

    private LectureAchievementRow mutable(Long achievementId) {
        LectureAchievementRow current = mapper.findById(achievementId);
        if (current == null) throw new NotFoundException("강의실적을 찾을 수 없습니다.");
        if ("EVALUATION_CONFIRMED".equals(current.certificationStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정된 실적은 변경할 수 없습니다.");
        }
        return current;
    }

    private LectureAchievementSearchCriteria normalizeCriteria(
            LectureAchievementSearchCriteria criteria,
            CurrentUser actor
    ) {
        LectureAchievementSearchCriteria source = criteria == null
                ? new LectureAchievementSearchCriteria(0, 20, null, null, null, null, null, null)
                : criteria;
        String role = actor.roles().contains("R04") || actor.roles().contains("R09") ? "R04" : actor.roles().contains("R02") ? "R02" : "R01";
        return new LectureAchievementSearchCriteria(
                source.page(), source.size(), text(source.managementNo()), text(source.teacherName()),
                text(source.managementItemCode()), text(source.certificationStatus()), actor.userId(), role
        );
    }

    private void requireRole(CurrentUser actor) {
        if (actor == null || actor.roles() == null || actor.roles().stream().noneMatch(
                role -> role.equals("R01") || role.equals("R02") || role.equals("R04") || role.equals("R09")
        )) throw new ForbiddenException();
    }

    private String year(String requested, LocalDate occurredDate) {
        String value = text(requested);
        return value == null && occurredDate != null ? String.valueOf(occurredDate.getYear()) : value;
    }

    private String organization(String requested, Long targetUserId) {
        String value = text(requested);
        return value == null ? mapper.findActiveOrganizationCode(targetUserId) : value.toUpperCase();
    }

    private String detail(SaveLectureAchievementRequest request) {
        try {
            return request.achievementDetail() == null ? "{}" : objectMapper.writeValueAsString(request.achievementDetail());
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "강의실적 상세정보 형식이 올바르지 않습니다.",
                    List.of(new ValidationError("achievementDetail", "상세정보를 확인하세요."))
            );
        }
    }

    private String text(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private String reasonOrDefault(String reason) { return reason == null ? "강의실적 처리" : reason; }
}
