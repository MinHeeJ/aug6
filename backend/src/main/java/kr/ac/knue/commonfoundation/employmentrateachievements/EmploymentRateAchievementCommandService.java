package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
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
import kr.ac.knue.commonfoundation.employmentrateachievement.EmploymentRateAchievementRequest;
import kr.ac.knue.commonfoundation.employmentrateachievement.EmploymentRateAchievementRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists individual employment-rate achievement commands through the
 * approved table and shared education guards. The source-row mutation and
 * audit history write are kept in the same transaction.
 */
@Service
public class EmploymentRateAchievementCommandService {
    private final JdbcTemplate jdbcTemplate;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public EmploymentRateAchievementCommandService(
            JdbcTemplate jdbcTemplate,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Creates a caller-owned draft after authorization, period, and finalization checks. */
    @Transactional
    public EmploymentRateAchievementRow create(
            EmploymentRateAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriter(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        Long achievementId = jdbcTemplate.queryForObject(
                """
                INSERT INTO employment_rate_achievements (
                    management_no,
                    target_user_id,
                    evaluation_year,
                    management_item_code,
                    achievement_date,
                    achievement_name,
                    attachment_ids,
                    achievement_status,
                    deleted_yn,
                    created_by,
                    updated_by
                )
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), 'DRAFT', 'N', ?, ?)
                RETURNING achievement_id
                """,
                Long.class,
                "ER-" + UUID.randomUUID(),
                requester.userId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                blankToNull(request.achievementName()),
                attachmentIdsJson(request.attachmentIds()),
                requester.userId(),
                requester.userId());
        EmploymentRateAchievementRow saved = find(achievementId);
        insertChangeHistory(saved, "CREATE", null, saved.achievementName(), requester.userId(), requestId);
        return saved;
    }

    /** Updates a caller-owned source row only after the shared mutable-state guard succeeds. */
    @Transactional
    public EmploymentRateAchievementRow update(
            Long achievementId,
            EmploymentRateAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriter(requester);
        EmploymentRateAchievementRow existing = find(achievementId);
        if (!requester.userId().equals(existing.targetUserId())) {
            throw new ForbiddenException();
        }
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.targetUserId(),
                        String.valueOf(Year.from(existing.achievementDate())),
                        request.achievementDate()));
        int changed = jdbcTemplate.update(
                """
                UPDATE employment_rate_achievements
                SET management_item_code = ?,
                    achievement_date = ?,
                    achievement_name = ?,
                    attachment_ids = CAST(? AS jsonb),
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = ?
                WHERE achievement_id = ?
                    AND deleted_yn = 'N'
                """,
                request.managementItemCode().trim(),
                request.achievementDate(),
                blankToNull(request.achievementName()),
                attachmentIdsJson(request.attachmentIds()),
                requester.userId(),
                achievementId);
        if (changed != 1) {
            throw new NotFoundException("수정할 취업률 실적을 찾을 수 없습니다.");
        }
        EmploymentRateAchievementRow saved = find(achievementId);
        insertChangeHistory(
                existing,
                "UPDATE",
                existing.achievementName(),
                saved.achievementName(),
                requester.userId(),
                requestId);
        return saved;
    }

    private EmploymentRateAchievementRow find(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new BusinessValidationException(
                    "취업률 실적 식별자가 올바르지 않습니다.",
                    List.of(new ValidationError("achievementId", "실적 식별자를 입력하세요.")));
        }
        List<EmploymentRateAchievementRow> rows = jdbcTemplate.query(
                """
                SELECT achievement_id,
                       target_user_id,
                       management_item_code,
                       achievement_date,
                       achievement_name,
                       attachment_ids::text AS attachment_ids_json,
                       achievement_status,
                       created_at,
                       updated_at
                FROM employment_rate_achievements
                WHERE achievement_id = ?
                    AND deleted_yn = 'N'
                """,
                (resultSet, rowNumber) -> new EmploymentRateAchievementRow(
                        resultSet.getLong("achievement_id"),
                        resultSet.getLong("target_user_id"),
                        resultSet.getString("management_item_code"),
                        resultSet.getObject("achievement_date", LocalDate.class),
                        resultSet.getString("achievement_name"),
                        attachmentIds(resultSet.getString("attachment_ids_json")),
                        resultSet.getString("achievement_status"),
                        resultSet.getObject("created_at", LocalDateTime.class),
                        resultSet.getObject("updated_at", LocalDateTime.class)),
                achievementId);
        if (rows.isEmpty()) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        return rows.get(0);
    }

    private void insertChangeHistory(
            EmploymentRateAchievementRow row,
            String changeType,
            String beforeValue,
            String afterValue,
            Long changedBy,
            String requestId) {
        jdbcTemplate.update(
                """
                INSERT INTO data_change_histories (
                    target_business,
                    target_key,
                    change_type,
                    field_name,
                    before_value,
                    after_value,
                    changed_by,
                    change_reason
                )
                VALUES (?, ?, ?, 'achievement_name', ?, ?, ?, ?)
                """,
                "employment_rate_achievements",
                String.valueOf(row.achievementId()),
                changeType,
                beforeValue,
                afterValue,
                changedBy,
                "취업률 실적 " + ("CREATE".equals(changeType) ? "저장" : "수정") + " requestId=" + requestId);
    }

    private void validate(EmploymentRateAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "취업률 실적 정보를 입력하세요."));
        } else {
            if (blankToNull(request.managementItemCode()) == null) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.achievementDate() == null) {
                errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("취업률 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private void requireWriter(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private List<String> attachmentIds(String attachmentIdsJson) {
        try {
            return attachmentIdsJson == null
                    ? List.of()
                    : objectMapper.readValue(attachmentIdsJson, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 첨부 참조값을 읽을 수 없습니다.", exception);
        }
    }

    private String attachmentIdsJson(List<String> attachmentIds) {
        try {
            List<String> normalized = attachmentIds == null
                    ? List.of()
                    : attachmentIds.stream()
                            .filter(id -> id != null && !id.isBlank())
                            .map(String::trim)
                            .toList();
            return objectMapper.writeValueAsString(normalized);
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
