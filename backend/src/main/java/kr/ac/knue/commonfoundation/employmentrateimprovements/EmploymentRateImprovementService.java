package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped reads and atomic header/detail/status/audit writes for employment-rate improvements. */
@Service
public class EmploymentRateImprovementService {
    private static final Set<String> EDITABLE = Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED");
    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementGuardMapper guard;
    private final ObjectMapper json;

    public EmploymentRateImprovementService(
            EmploymentRateImprovementMapper mapper, EducationAchievementGuardMapper guard, ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.json = json;
    }

    /** Uses the same XML scope/filter fragments for list and total, including multi-role union. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(EmploymentRateImprovementSearch search, CurrentUser user) {
        requireRole(user, false);
        requireFunction(user, "READ");
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(search, user.userId(), user.roles()), search.page(), search.pageSize(),
                mapper.count(search, user.userId(), user.roles()), mapper.managementItemCodes(),
                allowed(user, "CREATE"), allowed(user, "UPDATE"));
    }

    /** Detail is freshly loaded and has exactly the same visibility boundary as list. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        requireFunction(user, "READ");
        EmploymentRateImprovementRow row = existing(id, false);
        if (mapper.visible(id, user.userId(), user.roles()) == 0) throw new ForbiddenException();
        return row;
    }

    /** Generated key is consumed only after inserting the header; readback follows detail insertion. */
    @Transactional
    public EmploymentRateImprovementSaveResult create(
            EmploymentRateImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        String year = body.evaluationYear() == null
                ? String.valueOf(body.achievementDate().getYear()) : body.evaluationYear();
        requirePeriodAndFinalization(user.userId(), year);
        requireFunction(user, "CREATE");
        validate(body);
        if (body.achievementStatus() != null && body.achievementStatus() != EducationAchievementStatus.DRAFT) {
            throw new EmploymentRateImprovementConflict("INVALID_STATE_TRANSITION");
        }
        String organization = mapper.organization(user.userId());
        if (organization == null) throw new ForbiddenException();
        Map<String, Object> values = new HashMap<>();
        values.put("managementNo", "ERI-" + UUID.randomUUID());
        values.put("userId", user.userId());
        values.put("organization", organization);
        values.put("year", year);
        values.put("body", body);
        mapper.insertHeader(values);
        Long id = ((Number) values.get("id")).longValue();
        mapper.insertDetail(id, body);
        EmploymentRateImprovementRow saved = existing(id, false);
        mapper.statusHistory(id, null, "DRAFT", user.userId(), requestId);
        mapper.history(id, "CREATE", null, snapshot(saved), user.userId(), requestId);
        return result(saved);
    }

    /** Locks before mutation, preserves original year/owner and records complete before/after snapshots. */
    @Transactional
    public EmploymentRateImprovementSaveResult update(
            Long id, EmploymentRateImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        EmploymentRateImprovementRow before = existing(id, true);
        if (!user.roles().contains("R09") && !user.userId().equals(before.teacherUserId())) {
            throw new ForbiddenException();
        }
        requirePeriodAndFinalization(before.teacherUserId(), before.evaluationYear());
        if ("EVALUATION_CONFIRMED".equals(before.achievementStatus())) {
            throw new EmploymentRateImprovementConflict("CONFIRMED_DATA_LOCKED");
        }
        if (!EDITABLE.contains(before.achievementStatus())) {
            throw new EmploymentRateImprovementConflict("INVALID_STATE_TRANSITION");
        }
        requireFunction(user, "UPDATE");
        validate(body);
        if (body.evaluationYear() != null && !before.evaluationYear().equals(body.evaluationYear())) {
            invalid("evaluationYear", "수정 시 기존 평가연도를 유지해야 합니다.");
        }
        String next = body.achievementStatus() == null ? before.achievementStatus() : body.achievementStatus().name();
        if (!next.equals(before.achievementStatus()) && !"SUBMITTED".equals(next)) {
            throw new EmploymentRateImprovementConflict("INVALID_STATE_TRANSITION");
        }
        mapper.updateHeader(id, body, next, user.userId());
        mapper.updateDetail(id, body);
        EmploymentRateImprovementRow saved = existing(id, false);
        if (!next.equals(before.achievementStatus())) {
            mapper.statusHistory(id, before.achievementStatus(), next, user.userId(), requestId);
        }
        mapper.history(id, "UPDATE", snapshot(before), snapshot(saved), user.userId(), requestId);
        return result(saved);
    }

    private void requirePeriodAndFinalization(Long userId, String year) {
        if (guard.countActiveInputPeriods(year, userId) == 0) {
            throw new EmploymentRateImprovementConflict("PERIOD_NOT_ACTIVE");
        }
        if (guard.countEvaluationConfirmations(userId, year) > 0) {
            throw new EmploymentRateImprovementConflict("CONFIRMED_DATA_LOCKED");
        }
    }

    private EmploymentRateImprovementSaveResult result(EmploymentRateImprovementRow row) {
        boolean warning = guard.countEvaluationDatePeriods(
                row.evaluationYear(), row.teacherUserId(), row.achievementDate()) == 0;
        return new EmploymentRateImprovementSaveResult(
                row, warning, warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 완료되었습니다." : null);
    }

    private void validate(EmploymentRateImprovementRequest body) {
        if (!mapper.managementItemCodes().contains(body.managementItemCode())) {
            invalid("managementItemCode", "활성 교육영역 관리항목을 선택하세요.");
        }
        if (body.specialLectureEndDate().isBefore(body.specialLectureStartDate())) {
            invalid("specialLectureEndDate", "특강 종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (body.attachmentRef() != null && !body.attachmentRef().matches("[A-Za-z0-9_.:-]{1,300}")) {
            invalid("attachmentRef", "파일 경로가 아닌 첨부 참조를 입력하세요.");
        }
    }

    private EmploymentRateImprovementRow existing(Long id, boolean lock) {
        EmploymentRateImprovementRow row = mapper.find(id, lock);
        if (row == null) throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        return row;
    }

    public static void requireRole(CurrentUser user, boolean write) {
        if (user == null) throw new UnauthenticatedException();
        List<String> roles = write ? List.of("R01", "R09") : List.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(roles::contains)) throw new ForbiddenException();
    }

    private boolean allowed(CurrentUser user, String function) {
        if (user.roles().contains("R09")) return true;
        boolean write = !"READ".equals(function);
        List<String> eligible = user.roles().stream()
                .filter(role -> write ? "R01".equals(role) : List.of("R01", "R02", "R04").contains(role))
                .toList();
        return !eligible.isEmpty() && mapper.functionAllowed(eligible, function) > 0;
    }

    private void requireFunction(CurrentUser user, String function) {
        if (!allowed(user, function)) throw new ForbiddenException();
    }

    private void invalid(String field, String message) {
        throw new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }

    private String snapshot(EmploymentRateImprovementRow row) {
        try {
            return json.writeValueAsString(row);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("실적 이력을 기록하지 못했습니다.", exception);
        }
    }
}
