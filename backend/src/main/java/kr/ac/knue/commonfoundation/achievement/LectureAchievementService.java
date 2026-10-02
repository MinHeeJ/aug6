package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the BASIC-79 lecture-achievement list and save workflow through the shared education
 * achievement guard, preserving status and data-change history in the same transaction.
 */
@Service
public class LectureAchievementService {
    private final LectureAchievementMapper mapper;
    private final EducationAchievementGuard guard;
    private final ObjectMapper objectMapper;

    public LectureAchievementService(
            LectureAchievementMapper mapper,
            EducationAchievementGuard guard,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.guard = guard;
        this.objectMapper = objectMapper;
    }

    /** Returns a filtered, deterministically ordered page of non-deleted lecture-achievement rows. */
    @Transactional(readOnly = true)
    public LectureAchievementModels.SearchResponse list(
            LectureAchievementModels.SearchCriteria criteria,
            CurrentUser user
    ) {
        LectureAchievementModels.SearchCriteria requested = criteria == null
                ? new LectureAchievementModels.SearchCriteria(0, 20, null, null, null, null, null, null)
                : criteria;
        LectureAchievementModels.SearchCriteria normalized = new LectureAchievementModels.SearchCriteria(
                Math.max(requested.page(), 0),
                requested.safeSize(),
                requested.managementNo(),
                requested.teacherName(),
                requested.managementItemCode(),
                requested.occurredDateFrom(),
                requested.occurredDateTo(),
                requested.certificationStatus()
        );
        boolean managerScope = user != null
                && (user.roles().contains("R02") || user.roles().contains("R04"));
        return new LectureAchievementModels.SearchResponse(
                mapper.list(normalized, user.userId(), managerScope),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.count(normalized, user.userId(), managerScope)
        );
    }

    /**
     * Saves one lecture-achievement source row after the shared authorization and lifecycle checks.
     * A supplied action records the corresponding approved status transition atomically.
     */
    @Transactional
    public LectureAchievementModels.Row save(
            LectureSaveRequest request,
            CurrentUser user
    ) {
        validateRequest(request);
        normalizeOwnership(request, user);
        LectureAchievementModels.Row before = request.getAchievementId() == null
                ? null
                : mapper.findById(request.getAchievementId());
        if (request.getAchievementId() != null && before == null) {
            throw new NotFoundException("수정할 강의실적을 찾을 수 없습니다.");
        }
        if (before != null) {
            request.setTeacherUserId(before.teacherUserId());
            request.setOrganizationCode(before.organizationCode());
            request.setEvaluationYear(before.evaluationYear());
        }

        EducationAchievementGuardResult guardResult = guard.validateMutation(
                user,
                new EducationAchievementCommandContext(
                        request.getTeacherUserId(),
                        request.getEvaluationYear(),
                        request.getOrganizationCode(),
                        request.getOccurredDate(),
                        before == null ? "DRAFT" : before.certificationStatus()
                )
        );
        String previousStatus = before == null ? null : before.certificationStatus();
        EducationAchievementTransition transition = resolveTransition(previousStatus, request);
        String nextStatus = transition == null
                ? (before == null ? "DRAFT" : previousStatus)
                : transition.nextStatus();
        String detailJson = serializeDetail(request);

        if (before == null) {
            mapper.insert(request, detailJson, nextStatus, user.userId());
            mapper.insertStatusHistory(
                    request.getAchievementId(),
                    null,
                    nextStatus,
                    transition == null ? "CREATE" : transition.actionType(),
                    transition == null ? null : transition.reasonCode(),
                    transition == null ? null : transition.opinion(),
                    user.userId()
            );
            mapper.insertChangeHistory(
                    request.getAchievementId(),
                    "CREATE",
                    null,
                    detailJson,
                    user.userId(),
                    "강의실적 등록"
            );
        } else {
            mapper.update(
                    request.getAchievementId(),
                    request.getManagementItemCode().trim(),
                    request.getOccurredDate(),
                    detailJson,
                    trimToNull(request.getAttachmentRef()),
                    nextStatus,
                    user.userId()
            );
            if (transition != null) {
                mapper.insertStatusHistory(
                        request.getAchievementId(),
                        previousStatus,
                        nextStatus,
                        transition.actionType(),
                        transition.reasonCode(),
                        transition.opinion(),
                        user.userId()
                );
            }
            mapper.insertChangeHistory(
                    request.getAchievementId(),
                    "UPDATE",
                    before.achievementDetail(),
                    detailJson,
                    user.userId(),
                    "강의실적 수정"
            );
        }
        LectureAchievementModels.Row saved = mapper.findById(request.getAchievementId());
        if (saved == null) {
            throw new NotFoundException("저장한 강의실적을 재조회하지 못했습니다.");
        }
        return new LectureAchievementModels.Row(
                saved.achievementId(),
                saved.managementNo(),
                saved.teacherUserId(),
                saved.teacherName(),
                saved.organizationCode(),
                saved.evaluationYear(),
                saved.managementItemCode(),
                saved.occurredDate(),
                saved.achievementDetail(),
                saved.certificationStatus(),
                saved.attachmentRef(),
                guardResult.occurredDateOutOfRangeWarning(),
                saved.updatedAt()
        );
    }

    private void validateRequest(LectureSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("body", "저장 요청을 입력하세요."));
        } else {
            if (trimToNull(request.getManagementItemCode()) == null) {
                fields.add(new ValidationError("managementItemCode", "관리항목을 선택하세요."));
            }
            if (request.getOccurredDate() == null) {
                fields.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
            }
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("강의실적 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private void normalizeOwnership(LectureSaveRequest request, CurrentUser user) {
        if (user == null) {
            return;
        }
        if (request.getTeacherUserId() == null) {
            request.setTeacherUserId(user.userId());
        }
        if (trimToNull(request.getOrganizationCode()) == null) {
            request.setOrganizationCode(mapper.findOrganizationCodeForUser(request.getTeacherUserId()));
        }
        if (trimToNull(request.getEvaluationYear()) == null) {
            request.setEvaluationYear(String.valueOf(request.getOccurredDate().getYear()));
        }
    }

    private EducationAchievementTransition resolveTransition(
            String previousStatus,
            LectureSaveRequest request
    ) {
        String actionType = trimToNull(request.getActionType());
        if (actionType == null) {
            return null;
        }
        return guard.validateTransition(
                previousStatus == null ? "DRAFT" : previousStatus,
                actionType,
                request.getReasonCode(),
                request.getOpinion()
        );
    }

    private String serializeDetail(LectureSaveRequest request) {
        try {
            return request.getAchievementDetail() == null
                    ? "{}"
                    : objectMapper.writeValueAsString(request.getAchievementDetail());
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "강의 상세 입력값을 처리할 수 없습니다.",
                    List.of(new ValidationError("achievementDetail", "상세 입력값 형식이 올바르지 않습니다."))
            );
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
