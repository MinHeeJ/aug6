package kr.ac.knue.commonfoundation.faculty.achievement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the lecture-evaluation achievement use case through the approved guard chain,
 * source persistence, append-only status history, and operational change history.
 */
@Service
public class EducationAchievementService {
    private static final Set<String> ACHIEVEMENT_ROLES = Set.of("R01", "R02", "R04", "R09");
    private final EducationAchievementMapper mapper;
    private final ObjectMapper objectMapper;
    private final EducationAchievementGuardChain guardChain = new EducationAchievementGuardChain();

    public EducationAchievementService(
            EducationAchievementMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Lists non-deleted lecture-evaluation sources with dynamic filters and role-aware scope.
     */
    @Transactional(readOnly = true)
    public LectureEvaluationAchievementSearchResponse listLectureEvaluationAchievements(
            LectureEvaluationAchievementSearchCriteria criteria,
            CurrentUser currentUser
    ) {
        requireAchievementRole(currentUser);
        validateSearchCriteria(criteria);
        boolean restrictToSelf = currentUser.roles().contains("R01")
                && !currentUser.roles().contains("R02")
                && !currentUser.roles().contains("R04");
        boolean unrestricted = currentUser.roles().contains("R09");
        List<LectureEvaluationAchievementRow> rows = mapper.listLectureEvaluationAchievements(
                criteria,
                currentUser.userId(),
                restrictToSelf,
                unrestricted
        );
        return new LectureEvaluationAchievementSearchResponse(
                rows,
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countLectureEvaluationAchievements(
                        criteria,
                        currentUser.userId(),
                        restrictToSelf,
                        unrestricted
                )
        );
    }

    /**
     * Creates the OpenAPI-defined source record and records its initial DRAFTING state atomically.
     * The API contract contains no identifier, so updates are intentionally not inferred here.
     */
    @Transactional
    public SaveLectureEvaluationAchievementResult saveLectureEvaluationAchievement(
            SaveLectureEvaluationAchievementRequest request,
            CurrentUser currentUser,
            String requestId
    ) {
        requireAchievementRole(currentUser);
        if (request == null) {
            throw new BusinessValidationException(
                    "강의평가 실적 저장 요청이 필요합니다.",
                    List.of(new ValidationError("request", "저장할 강의평가 실적을 입력하세요."))
            );
        }
        List<ValidationError> requiredFields = validateRequiredFields(request);
        if (!requiredFields.isEmpty()) {
            throw new BusinessValidationException("강의평가 실적 입력값이 올바르지 않습니다.", requiredFields);
        }
        LocalDate occurredDate = request.getOccurredDate();
        String evaluationYear = occurredDate == null ? null : String.valueOf(occurredDate.getYear());
        Long targetUserId = currentUser.userId();
        boolean dataScopeAllowed = targetUserId != null
                && (targetUserId.equals(currentUser.userId())
                || mapper.isAchievementDataScopeAllowed(currentUser.userId(), targetUserId) > 0);
        boolean inputPeriodActive = evaluationYear != null && mapper.hasActiveInputPeriod(evaluationYear) > 0;
        boolean evaluationConfirmed = evaluationYear != null
                && mapper.isEvaluationConfirmed(targetUserId, evaluationYear) > 0;
        EducationAchievementEvaluationPeriod evaluationPeriod = evaluationYear == null
                ? null
                : mapper.findEvaluationPeriod(evaluationYear);
        EducationAchievementGuardChain.EducationAchievementGuardResult guardResult = guardChain.validateWrite(
                currentUser,
                targetUserId,
                dataScopeAllowed,
                inputPeriodActive,
                evaluationConfirmed,
                occurredDate,
                evaluationPeriod == null ? null : evaluationPeriod.startDate(),
                evaluationPeriod == null ? null : evaluationPeriod.endDate()
        );
        List<ValidationError> fields = validateSaveRequest(request, evaluationYear);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("강의평가 실적 입력값이 올바르지 않습니다.", fields);
        }

        String managementItemCode = request.getManagementItemCode().trim().toUpperCase();
        if (mapper.existsActiveManagementItem(managementItemCode, evaluationYear) == 0) {
            throw new BusinessValidationException(
                    "강의평가 실적 입력값이 올바르지 않습니다.",
                    List.of(new ValidationError("managementItemCode", "활성화된 관리항목 코드를 입력하세요."))
            );
        }
        String detailJson = serializeDetail(request.getAchievementDetail());
        LectureEvaluationAchievementRow saved = mapper.insertLectureEvaluationAchievement(
                evaluationYear,
                targetUserId,
                managementItemCode,
                occurredDate,
                detailJson,
                currentUser.userId()
        );
        EducationAchievementGuardChain.EducationAchievementStatusHistory initialHistory =
                new EducationAchievementGuardChain.EducationAchievementStatusHistory(
                        "LECTURE_EVALUATION",
                        saved.achievementId(),
                        null,
                        "DRAFTING",
                        "CREATE",
                        null,
                        "강의평가 실적 등록",
                        currentUser.userId(),
                        guardResult.evaluatedAt()
                );
        mapper.insertEducationAchievementStatusHistory(initialHistory, currentUser.userId());
        mapper.insertChangeHistory(
                "lecture_evaluation_achievements",
                String.valueOf(saved.achievementId()),
                "CREATE",
                "achievement",
                null,
                detailJson,
                currentUser.userId(),
                "강의평가 실적 등록",
                requestId
        );
        return new SaveLectureEvaluationAchievementResult(saved, guardResult.occurredDateWarning());
    }

    private List<ValidationError> validateRequiredFields(
            SaveLectureEvaluationAchievementRequest request
    ) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.getManagementItemCode() == null || request.getManagementItemCode().isBlank()) {
            fields.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
        }
        if (request.getOccurredDate() == null) {
            fields.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
        }
        return fields;
    }

    private List<ValidationError> validateSaveRequest(
            SaveLectureEvaluationAchievementRequest request,
            String evaluationYear
    ) {
        List<ValidationError> fields = validateRequiredFields(request);
        if (evaluationYear == null) {
            return fields;
        }
        if (!evaluationYear.matches("^[0-9]{4}$")) {
            fields.add(new ValidationError("occurredDate", "업적발생일을 올바르게 입력하세요."));
        }
        return fields;
    }

    private void validateSearchCriteria(LectureEvaluationAchievementSearchCriteria criteria) {
        List<ValidationError> fields = new ArrayList<>();
        if (criteria.size() != 20 && criteria.size() != 50 && criteria.size() != 100) {
            fields.add(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요."));
        }
        if (criteria.occurredDateFrom() != null && criteria.occurredDateTo() != null
                && criteria.occurredDateFrom().isAfter(criteria.occurredDateTo())) {
            fields.add(new ValidationError("occurredDateTo", "종료일은 시작일보다 빠를 수 없습니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("강의평가 실적 검색조건이 올바르지 않습니다.", fields);
        }
    }

    private void requireAchievementRole(CurrentUser currentUser) {
        if (currentUser == null || currentUser.roles() == null
                || currentUser.roles().stream().noneMatch(ACHIEVEMENT_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private String serializeDetail(Map<String, Object> achievementDetail) {
        try {
            return objectMapper.writeValueAsString(
                    achievementDetail == null ? Map.of() : achievementDetail
            );
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "강의평가 세부 입력값이 올바르지 않습니다.",
                    List.of(new ValidationError("achievementDetail", "세부 입력값을 저장할 수 없습니다."))
            );
        }
    }
}
