package com.example.faculty.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns lecture-evaluation list, save, transition, and audit orchestration through the shared
 * education-achievement guard and the real PostgreSQL mapper boundary.
 */
@Service
public class LectureEvaluationAchievementService {
    private static final String ACHIEVEMENT_TYPE = "LECTURE_EVALUATION";
    private final LectureEvaluationAchievementMapper mapper;
    private final EducationAchievementGuardChain guardChain;

    public LectureEvaluationAchievementService(
            LectureEvaluationAchievementMapper mapper,
            EducationAchievementGuardChain guardChain
    ) {
        this.mapper = mapper;
        this.guardChain = guardChain;
    }

    /**
     * Returns only the lecture-evaluation rows visible to the requester's supported role and data scope.
     */
    @Transactional(readOnly = true)
    public LectureEvaluationAchievementSearchResponse list(
            LectureEvaluationAchievementSearchCriteria criteria,
            CurrentUser user
    ) {
        requireAchievementRole(user);
        validateSearch(criteria);
        boolean restrictToRequester = user.roles().contains("R01")
                && !user.roles().contains("R02")
                && !user.roles().contains("R04");
        LectureEvaluationAchievementSearchCriteria scoped = new LectureEvaluationAchievementSearchCriteria(
                criteria.safePage(),
                criteria.safePageSize(),
                trimToNull(criteria.managementNo()),
                trimToNull(criteria.teacherName()),
                trimToNull(criteria.managementItemCode()),
                criteria.occurredDateFrom(),
                criteria.occurredDateTo(),
                trimToNull(criteria.certificationStatus()),
                user.userId(),
                restrictToRequester
        );
        return new LectureEvaluationAchievementSearchResponse(
                mapper.list(scoped),
                scoped.safePage(),
                scoped.safePageSize(),
                mapper.count(scoped)
        );
    }

    /**
     * Saves one source row, records changed values, and appends state history in the same transaction.
     */
    @Transactional
    public LectureEvaluationAchievementSaveResult save(
            SaveLectureEvaluationAchievementRequest request,
            CurrentUser user
    ) {
        requireAchievementRole(user);
        validateSave(request);
        LectureEvaluationAchievementRow before = request.achievementId() == null
                ? null
                : requireExisting(request.achievementId());
        boolean dataScopeAllowed = before == null
                || !user.roles().contains("R01")
                || user.roles().contains("R02")
                || user.roles().contains("R04")
                || user.userId().equals(before.teacherUserId());
        String evaluationYear = String.valueOf(request.occurredDate().getYear());
        EducationAchievementGuardResult guardResult = guardChain.validateMutation(
                new EducationAchievementMutationContext(
                        Set.copyOf(user.roles()),
                        dataScopeAllowed,
                        mapper.hasActiveInputPeriod(evaluationYear),
                        before == null ? "DRAFTING" : before.certificationStatus(),
                        request.occurredDate(),
                        mapper.findEvaluationPeriodStart(evaluationYear),
                        mapper.findEvaluationPeriodEnd(evaluationYear)
                )
        );
        LectureEvaluationAchievementCommand command = new LectureEvaluationAchievementCommand(
                request.achievementId(),
                before == null ? generatedManagementNo() : before.managementNo(),
                before == null ? user.userId() : before.teacherUserId(),
                evaluationYear,
                before == null ? null : before.organizationCode(),
                request.managementItemCode().trim(),
                request.occurredDate(),
                detailJson(request.achievementDetail()),
                trimToNull(request.attachmentRef()),
                before == null ? "DRAFTING" : before.certificationStatus(),
                user.userId()
        );
        LectureEvaluationAchievementRow saved;
        if (before == null) {
            mapper.insert(command);
            saved = mapper.findByManagementNo(command.managementNo());
        } else {
            mapper.update(command);
            saved = requireExisting(command.achievementId());
        }
        recordChange(before, saved, user.userId());
        if (hasText(request.transitionAction())) {
            saved = transition(saved, request, user.userId());
        }
        return new LectureEvaluationAchievementSaveResult(
                saved,
                guardResult.warning(),
                guardResult.warningCode()
        );
    }

    private LectureEvaluationAchievementRow transition(
            LectureEvaluationAchievementRow current,
            SaveLectureEvaluationAchievementRequest request,
            Long userId
    ) {
        String actionType = request.transitionAction().trim().toUpperCase();
        String nextStatus = nextStatusFor(actionType);
        EducationAchievementStatusHistory history = guardChain.validateTransition(
                new EducationAchievementTransitionRequest(
                        ACHIEVEMENT_TYPE,
                        current.achievementId(),
                        current.certificationStatus(),
                        nextStatus,
                        actionType,
                        trimToNull(request.reasonCode()),
                        trimToNull(request.opinion()),
                        userId
                )
        );
        mapper.updateCertificationStatus(current.achievementId(), history.nextStatus(), userId);
        mapper.insertStatusHistory(history);
        LectureEvaluationAchievementRow transitioned = requireExisting(current.achievementId());
        mapper.insertChangeHistory(
                "lecture_evaluation_achievements",
                String.valueOf(current.achievementId()),
                "UPDATE",
                "certification_status",
                current.certificationStatus(),
                transitioned.certificationStatus(),
                userId,
                transitionReason(request)
        );
        return transitioned;
    }

    private String nextStatusFor(String actionType) {
        return switch (actionType) {
            case "SUBMIT" -> "SUBMITTED";
            case "DEPARTMENT_CONFIRM" -> "DEPARTMENT_CONFIRMED";
            case "DEPARTMENT_REJECT" -> "DEPARTMENT_REJECTED";
            case "CERTIFY" -> "CERTIFIED";
            case "RETURN" -> "CERTIFICATION_RETURNED";
            case "CONFIRM" -> "EVALUATION_CONFIRMED";
            case "CANCEL" -> "CERTIFIED";
            default -> throw new BusinessValidationException(
                    "상태 전이 구분이 올바르지 않습니다.",
                    List.of(new ValidationError("transitionAction", "지원하지 않는 상태 전이 구분입니다."))
            );
        };
    }

    private void recordChange(
            LectureEvaluationAchievementRow before,
            LectureEvaluationAchievementRow after,
            Long userId
    ) {
        String beforeValue = before == null ? null : before.achievementDetailJson();
        mapper.insertChangeHistory(
                "lecture_evaluation_achievements",
                String.valueOf(after.achievementId()),
                before == null ? "CREATE" : "UPDATE",
                "achievement_detail",
                beforeValue,
                after.achievementDetailJson(),
                userId,
                before == null ? "강의평가 실적 등록" : "강의평가 실적 수정"
        );
    }

    private void validateSearch(LectureEvaluationAchievementSearchCriteria criteria) {
        List<ValidationError> fields = new ArrayList<>();
        if (criteria == null) {
            fields.add(new ValidationError("criteria", "검색조건을 입력하세요."));
        } else {
            if (criteria.page() < 0) {
                fields.add(new ValidationError("page", "페이지는 0 이상이어야 합니다."));
            }
            if (criteria.pageSize() != 20 && criteria.pageSize() != 50 && criteria.pageSize() != 100) {
                fields.add(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요."));
            }
            if (criteria.occurredDateFrom() != null
                    && criteria.occurredDateTo() != null
                    && criteria.occurredDateFrom().isAfter(criteria.occurredDateTo())) {
                fields.add(new ValidationError("occurredDateTo", "종료일은 시작일 이후여야 합니다."));
            }
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("강의평가 실적 검색조건이 올바르지 않습니다.", fields);
        }
    }

    private void validateSave(SaveLectureEvaluationAchievementRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("request", "저장 요청을 입력하세요."));
        } else {
            if (!hasText(request.managementItemCode())) {
                fields.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.occurredDate() == null) {
                fields.add(new ValidationError("occurredDate", "발생일을 입력하세요."));
            }
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("강의평가 실적 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private LectureEvaluationAchievementRow requireExisting(Long achievementId) {
        LectureEvaluationAchievementRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("강의평가 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void requireAchievementRole(CurrentUser user) {
        if (user == null || user.roles() == null || user.roles().stream().noneMatch(
                role -> role.equals("R01") || role.equals("R02") || role.equals("R04")
        )) {
            throw new ForbiddenException();
        }
    }

    private String detailJson(JsonNode detail) {
        return detail == null || detail.isNull() ? "{}" : detail.toString();
    }

    private String generatedManagementNo() {
        return "LE-" + UUID.randomUUID();
    }

    private String transitionReason(SaveLectureEvaluationAchievementRequest request) {
        return hasText(request.reasonCode()) ? request.reasonCode().trim()
                : hasText(request.opinion()) ? request.opinion().trim()
                : "강의평가 실적 상태 전이";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
