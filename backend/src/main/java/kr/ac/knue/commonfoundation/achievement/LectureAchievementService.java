package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the persistence-backed lecture list and save contract,
 * including mandatory status and data-change history writes in one transaction.
 */
@Service
public class LectureAchievementService {
    private static final Set<String> ALLOWED_ROLES = Set.of("R01", "R02", "R04", "R09");
    private static final Set<Integer> PAGE_SIZES = Set.of(20, 50, 100);
    private final LectureAchievementMapper mapper;
    private final ObjectMapper objectMapper;
    private final EducationAchievementValidationChain validationChain = new EducationAchievementValidationChain();

    public LectureAchievementService(LectureAchievementMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /** Lists only non-deleted lecture achievements using dynamically applied filters. */
    @Transactional(readOnly = true)
    public LectureAchievementSearchResponse list(CurrentUser user, String evaluationYear,
            String managementNo, String teacherName, String managementItemCode, LocalDate occurredDateFrom,
            LocalDate occurredDateTo, String certificationStatus, int page, int pageSize) {
        requireRole(user);
        if (occurredDateFrom != null && occurredDateTo != null && occurredDateFrom.isAfter(occurredDateTo)) {
            throw new BusinessValidationException("강의 조회기간이 올바르지 않습니다.",
                    List.of(new ValidationError("occurredDateFrom", "시작일은 종료일보다 늦을 수 없습니다.")));
        }
        int safePage = Math.max(page, 0);
        int safePageSize = PAGE_SIZES.contains(pageSize) ? pageSize : 20;
        LectureAchievementSearchCriteria criteria = new LectureAchievementSearchCriteria(
                blankToNull(evaluationYear), blankToNull(managementNo), blankToNull(teacherName),
                blankToNull(managementItemCode), occurredDateFrom, occurredDateTo, blankToNull(certificationStatus),
                safePageSize, safePage * safePageSize);
        return new LectureAchievementSearchResponse(mapper.list(criteria), safePage, safePageSize,
                mapper.count(criteria));
    }

    /**
     * Creates a DRAFTING achievement for the current user and records both its
     * initial status and immutable audit event before the transaction commits.
     */
    @Transactional
    public LectureAchievementSaveResponse save(CurrentUser user,
            SaveLectureAchievementRequest request, String requestId) {
        requireRole(user);
        validateSaveRequest(request);
        String evaluationYear = Integer.toString(request.occurredDate().getYear());
        EducationAchievementValidationChain.ValidationResult validation = validationChain.validate(
                new EducationAchievementValidationChain.ValidationContext(user, true, true, false,
                        request.occurredDate(), LocalDate.of(request.occurredDate().getYear(), 1, 1),
                        LocalDate.of(request.occurredDate().getYear(), 12, 31)));
        String managementNo = "LA-" + UUID.randomUUID();
        mapper.insertAchievement(managementNo, evaluationYear, user.userId(), user.name(),
                request.managementItemCode().trim(), request.occurredDate(), serializeDetail(request), user.userId());
        LectureAchievementRow saved = mapper.findByManagementNo(managementNo);
        mapper.insertStatusHistory(saved.achievementId(), user.userId());
        mapper.insertChangeHistory(saved.achievementId(), user.userId(), requestId);
        return new LectureAchievementSaveResponse(saved, validation.warnings());
    }

    /** Applies only the documented lifecycle edges and writes a durable transition history row. */
    @Transactional
    public LectureAchievementRow transition(CurrentUser user, Long achievementId,
            EducationAchievementTransitionRequest request) {
        requireRole(user);
        validationChain.validateTransitionInput(request);
        LectureAchievementRow current = mapper.findByIdForUpdate(achievementId);
        if (current == null) throw new NotFoundException("강의 실적을 찾을 수 없습니다.");
        if ("EVALUATION_CONFIRMED".equals(current.certificationStatus())) {
            throw new ConflictException("평가확정 데이터는 상태를 변경할 수 없습니다.");
        }
        String action = request.actionType().trim().toUpperCase();
        String nextStatus = nextStatus(current.certificationStatus(), action);
        mapper.updateCertificationStatus(achievementId, nextStatus, user.userId());
        mapper.insertTransitionStatusHistory(achievementId, current.certificationStatus(), nextStatus, action,
                request.reasonCode(), request.opinion(), user.userId());
        return mapper.findByIdForUpdate(achievementId);
    }

    private String nextStatus(String currentStatus, String action) {
        return switch (currentStatus + ":" + action) {
            case "DRAFTING:SUBMIT", "DEPARTMENT_REJECTED:SUBMIT", "CERTIFICATION_RETURNED:SUBMIT" -> "SUBMITTED";
            case "SUBMITTED:DEPARTMENT_CONFIRM" -> "DEPARTMENT_CONFIRMED";
            case "SUBMITTED:DEPARTMENT_REJECT" -> "DEPARTMENT_REJECTED";
            case "DEPARTMENT_CONFIRMED:CERTIFY" -> "CERTIFIED";
            case "DEPARTMENT_CONFIRMED:CERTIFICATION_REJECT" -> "CERTIFICATION_RETURNED";
            case "CERTIFIED:CONFIRM_EVALUATION" -> "EVALUATION_CONFIRMED";
            case "EVALUATION_CONFIRMED:CANCEL_EVALUATION" -> "CERTIFIED";
            default -> throw new ConflictException("허용되지 않은 강의 실적 상태 전이입니다.");
        };
    }

    private void requireRole(CurrentUser user) {
        if (user == null || user.roles() == null || user.roles().stream().noneMatch(ALLOWED_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void validateSaveRequest(SaveLectureAchievementRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || blankToNull(request.managementItemCode()) == null) {
            fields.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
        }
        if (request == null || request.occurredDate() == null) {
            fields.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("강의 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private String serializeDetail(SaveLectureAchievementRequest request) {
        try {
            return objectMapper.writeValueAsString(request.achievementDetail() == null
                    ? objectMapper.createObjectNode() : request.achievementDetail());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("강의 상세 입력값을 처리할 수 없습니다.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
